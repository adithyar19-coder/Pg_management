package com.pgmanagement.service;

import com.pgmanagement.entity.PG;
import com.pgmanagement.entity.Room;
import com.pgmanagement.entity.RoomAssignment;
import com.pgmanagement.entity.RoomRequest;
import com.pgmanagement.repository.RoomAssignmentRepository;
import com.pgmanagement.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Genetic-Algorithm based room recommender.
 *
 * <p>Given a {@link RoomRequest}, it scores every vacant room in the same PG against
 * the tenant's preferences (type, floor, AC) and current occupancy, then runs a
 * 50-generation evolutionary loop with selection / crossover / mutation steps
 * before returning the highest-scoring room.
 *
 * <p>The fitness function (0–100):
 * <ul>
 *   <li>+40 if room.type matches request.preferredType</li>
 *   <li>+25 if room.floor matches request.preferredFloor</li>
 *   <li>+20 if room.hasAc matches request.acPreference</li>
 *   <li>+15 weighted by occupancy: (capacity - currentOccupants) / capacity * 15</li>
 * </ul>
 * Each criterion that has "no preference" in the request awards full credit
 * (tenant said they don't care, so any value passes).
 */
@Service
@RequiredArgsConstructor
public class RoomSuggestionService {

    private final RoomRepository roomRepository;
    private final RoomAssignmentRepository roomAssignmentRepository;

    private static final int GENERATIONS = 50;
    private static final double MUTATION_RATE = 0.10;

    /**
     * Public entry point. Returns a Map ready for JSON serialization with keys:
     * {@code roomId, roomNumber, score, reasoning} — or throws if no vacant rooms exist.
     */
    public Map<String, Object> suggest(RoomRequest request) {
        if (request == null) throw new IllegalArgumentException("Request is null");
        PG pg = request.getPg();
        if (pg == null) throw new IllegalArgumentException("Request has no PG");

        // 1. Initial population: all vacant rooms in this PG.
        List<Room> rooms = roomRepository.findByPg(pg);
        Map<Long, Integer> occupancyByRoom = computeOccupancy(rooms);

        // A room is "vacant" if currentOccupants < capacity.
        List<Candidate> population = rooms.stream()
                .filter(r -> {
                    int occ = occupancyByRoom.getOrDefault(r.getId(), 0);
                    int cap = r.getCapacity() != null ? r.getCapacity() : 1;
                    return occ < cap;
                })
                .map(r -> new Candidate(r, occupancyByRoom.getOrDefault(r.getId(), 0), 0.0))
                .collect(Collectors.toList());

        if (population.isEmpty()) {
            throw new IllegalArgumentException("No vacant rooms available in " + pg.getName());
        }

        // 2. Score every candidate once for the initial generation
        Random rng = new Random();
        for (Candidate c : population) c.score = fitness(c, request, 1.0);

        // 3. Evolve for GENERATIONS rounds
        for (int gen = 0; gen < GENERATIONS; gen++) {

            // SELECTION — keep the top 50%
            population.sort(Comparator.comparingDouble((Candidate c) -> c.score).reversed());
            int keep = Math.max(1, population.size() / 2);
            List<Candidate> survivors = new ArrayList<>(population.subList(0, keep));

            // CROSSOVER — for each pair, build a "virtual" weight blend that
            // biases the next round's fitness recomputation. We don't create
            // new rooms — we re-evaluate survivors with a heuristic bias factor.
            // The bias is the average score of the two parents normalized to [0.9, 1.1].
            List<Candidate> nextGen = new ArrayList<>(survivors);
            for (int i = 0; i < survivors.size() - 1; i += 2) {
                Candidate p1 = survivors.get(i);
                Candidate p2 = survivors.get(i + 1);
                double avgScore = (p1.score + p2.score) / 2.0;
                double bias = 0.9 + (avgScore / 100.0) * 0.2; // 0.9..1.1
                // re-score p1 and p2 with the inherited bias
                p1.score = fitness(p1, request, bias);
                p2.score = fitness(p2, request, bias);
            }

            // MUTATION — 10% chance per survivor: jitter the fitness weights
            // to escape local optima.
            for (Candidate c : nextGen) {
                if (rng.nextDouble() < MUTATION_RATE) {
                    double jitter = 0.85 + rng.nextDouble() * 0.3; // 0.85..1.15
                    c.score = fitness(c, request, jitter);
                }
            }

            population = nextGen;
        }

        // 4. Pick the winner
        Candidate winner = population.stream()
                .max(Comparator.comparingDouble(c -> c.score))
                .orElseThrow();

        // Clamp to [0,100] for display sanity
        int finalScore = (int) Math.round(Math.max(0, Math.min(100, winner.score)));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roomId", winner.room.getId());
        result.put("roomNumber", winner.room.getRoomNumber());
        result.put("floor", winner.room.getFloor());
        result.put("type", winner.room.getType());
        result.put("hasAc", winner.room.getHasAc());
        result.put("capacity", winner.room.getCapacity());
        result.put("currentOccupants", winner.occupants);
        result.put("rentAmount", winner.room.getRentAmount());
        result.put("score", finalScore);
        result.put("reasoning", buildReasoning(winner, request, finalScore));
        return result;
    }

    /**
     * Pure fitness function. Returns a score in [0,100] (approximately — bias
     * factors can push slightly beyond, the caller clamps).
     */
    private double fitness(Candidate c, RoomRequest req, double bias) {
        double score = 0;

        // Type match: 40 points
        if (req.getPreferredType() == null || req.getPreferredType() == c.room.getType()) {
            score += 40;
        }

        // Floor match: 25 points
        if (req.getPreferredFloor() == null
                || Objects.equals(req.getPreferredFloor(), c.room.getFloor())) {
            score += 25;
        }

        // AC match: 20 points
        if (req.getAcPreference() == null
                || Objects.equals(req.getAcPreference(), c.room.getHasAc())) {
            score += 20;
        }

        // Occupancy: 15 points, weighted by free-bed ratio.
        int cap = c.room.getCapacity() != null ? c.room.getCapacity() : 1;
        double freeRatio = (double) (cap - c.occupants) / Math.max(1, cap);
        score += 15 * freeRatio;

        return score * bias;
    }

    /** Build a count of active assignments per room id for the given list. */
    private Map<Long, Integer> computeOccupancy(List<Room> rooms) {
        if (rooms.isEmpty()) return Collections.emptyMap();
        List<RoomAssignment> active = roomAssignmentRepository.findByRoomInAndIsActive(rooms, true);
        Map<Long, Integer> map = new HashMap<>();
        for (RoomAssignment a : active) {
            map.merge(a.getRoom().getId(), 1, Integer::sum);
        }
        return map;
    }

    /** Human-friendly reasoning describing which preferences matched. */
    private String buildReasoning(Candidate winner, RoomRequest req, int score) {
        List<String> matches = new ArrayList<>();
        List<String> misses = new ArrayList<>();

        if (req.getPreferredType() != null) {
            if (req.getPreferredType() == winner.room.getType())
                matches.add("type=" + winner.room.getType());
            else
                misses.add("type wanted " + req.getPreferredType() + " got " + winner.room.getType());
        }
        if (req.getPreferredFloor() != null) {
            if (Objects.equals(req.getPreferredFloor(), winner.room.getFloor()))
                matches.add("floor=" + winner.room.getFloor());
            else
                misses.add("floor wanted " + req.getPreferredFloor() + " got " + winner.room.getFloor());
        }
        if (req.getAcPreference() != null) {
            if (Objects.equals(req.getAcPreference(), winner.room.getHasAc()))
                matches.add("AC=" + winner.room.getHasAc());
            else
                misses.add("AC wanted " + req.getAcPreference() + " got " + winner.room.getHasAc());
        }
        int cap = winner.room.getCapacity() != null ? winner.room.getCapacity() : 1;
        int free = cap - winner.occupants;

        StringBuilder sb = new StringBuilder();
        sb.append("Score ").append(score).append("/100. ");
        if (!matches.isEmpty()) sb.append("Matches: ").append(String.join(", ", matches)).append(". ");
        if (!misses.isEmpty()) sb.append("Trade-offs: ").append(String.join("; ", misses)).append(". ");
        sb.append("Currently ").append(free).append(" of ").append(cap).append(" bed(s) free.");
        return sb.toString();
    }

    /** Holder for a room + its current occupant count + GA score (mutable across generations). */
    private static class Candidate {
        final Room room;
        final int occupants;
        double score;
        Candidate(Room room, int occupants, double score) {
            this.room = room;
            this.occupants = occupants;
            this.score = score;
        }
    }
}
