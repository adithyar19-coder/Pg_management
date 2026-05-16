import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { ForumPost, ForumReply, PG } from '../../core/models/models';

@Component({
  selector: 'app-community',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './community.component.html'
})
export class OwnerCommunityComponent implements OnInit {
  me = this.auth.getCurrentUser();
  pgs: PG[] = [];
  filterPgId: number | null = null;
  posts: ForumPost[] = [];
  loading = true;

  // Compose
  showCompose = false;
  composePgId: number | null = null;
  composeTitle = '';
  composeBody = '';
  posting = false;
  error = '';

  // Expanded thread state
  expandedId: number | null = null;
  replies: { [postId: number]: ForumReply[] } = {};
  newReply = '';
  replying = false;

  constructor(private api: ApiService, private auth: AuthService) {}

  ngOnInit() {
    this.api.getPGs().subscribe(r => {
      this.pgs = r.data || [];
      this.composePgId = this.pgs[0]?.id || null;
    });
    this.load();
  }

  load() {
    this.loading = true;
    this.api.getForumPostsOwner(this.filterPgId ?? undefined).subscribe({
      next: r => { this.posts = r.data || []; this.loading = false; },
      error: () => this.loading = false
    });
  }

  openCompose() {
    this.composeTitle = '';
    this.composeBody = '';
    this.error = '';
    this.showCompose = true;
  }

  submitPost() {
    if (!this.composeTitle.trim() || !this.composePgId) {
      this.error = 'Title and PG are required';
      return;
    }
    this.posting = true;
    this.api.createForumPostOwner(this.composePgId, this.composeTitle.trim(), this.composeBody.trim()).subscribe({
      next: () => { this.posting = false; this.showCompose = false; this.load(); },
      error: err => { this.posting = false; this.error = err.error?.message || 'Failed to post'; }
    });
  }

  toggleThread(p: ForumPost) {
    if (this.expandedId === p.id) {
      this.expandedId = null;
      return;
    }
    this.expandedId = p.id;
    this.newReply = '';
    if (!this.replies[p.id]) {
      this.api.getForumRepliesOwner(p.id).subscribe(r => this.replies[p.id] = r.data || []);
    }
  }

  sendReply(p: ForumPost) {
    if (!this.newReply.trim()) return;
    this.replying = true;
    this.api.addForumReplyOwner(p.id, this.newReply.trim()).subscribe({
      next: r => {
        this.replies[p.id] = [...(this.replies[p.id] || []), r.data];
        this.newReply = '';
        this.replying = false;
        // Update reply count locally
        p.replyCount = (p.replyCount || 0) + 1;
      },
      error: () => this.replying = false
    });
  }

  togglePin(p: ForumPost) {
    this.api.togglePinPost(p.id).subscribe(() => this.load());
  }

  deletePost(p: ForumPost) {
    if (!confirm(`Delete the post "${p.title}" and all its replies?`)) return;
    this.api.deleteForumPostOwner(p.id).subscribe(() => this.load());
  }

  deleteReply(p: ForumPost, r: ForumReply) {
    if (!confirm('Delete this reply?')) return;
    this.api.deleteForumReplyOwner(r.id).subscribe(() => {
      this.replies[p.id] = (this.replies[p.id] || []).filter(x => x.id !== r.id);
      p.replyCount = Math.max(0, (p.replyCount || 0) - 1);
    });
  }

  canDeleteReply(r: ForumReply): boolean {
    // Owner can delete any reply in their PG (server enforces this too)
    return true;
  }

  authorBadge(p: ForumPost | ForumReply): string {
    return p.author?.role === 'OWNER' ? '👑 Owner' : '👤 Tenant';
  }
}
