import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/services/api.service';
import { RoomAssignment } from '../../core/models/models';

@Component({
  selector: 'app-my-room',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-room.component.html'
})
export class MyRoomComponent implements OnInit {
  assignment: RoomAssignment | null = null;
  loading = true;

  constructor(private api: ApiService) {}
  ngOnInit() {
    this.api.getMyRoom().subscribe({
      next: r => { this.assignment = r.data || null; this.loading = false; },
      error: () => this.loading = false
    });
  }
}
