import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { ForumPost, ForumReply } from '../../core/models/models';

@Component({
  selector: 'app-tenant-community',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './community.component.html'
})
export class TenantCommunityComponent implements OnInit {
  me = this.auth.getCurrentUser();
  posts: ForumPost[] = [];
  loading = true;
  loadError = '';

  // Compose
  showCompose = false;
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

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.api.getForumPostsTenant().subscribe({
      next: r => { this.posts = r.data || []; this.loading = false; },
      error: err => {
        this.loadError = err.error?.message || 'Could not load posts';
        this.loading = false;
      }
    });
  }

  openCompose() {
    this.composeTitle = '';
    this.composeBody = '';
    this.error = '';
    this.showCompose = true;
  }

  submitPost() {
    if (!this.composeTitle.trim()) { this.error = 'Title is required'; return; }
    this.posting = true;
    this.api.createForumPostTenant(this.composeTitle.trim(), this.composeBody.trim()).subscribe({
      next: () => { this.posting = false; this.showCompose = false; this.load(); },
      error: err => { this.posting = false; this.error = err.error?.message || 'Failed to post'; }
    });
  }

  toggleThread(p: ForumPost) {
    if (this.expandedId === p.id) { this.expandedId = null; return; }
    this.expandedId = p.id;
    this.newReply = '';
    if (!this.replies[p.id]) {
      this.api.getForumRepliesTenant(p.id).subscribe(r => this.replies[p.id] = r.data || []);
    }
  }

  sendReply(p: ForumPost) {
    if (!this.newReply.trim()) return;
    this.replying = true;
    this.api.addForumReplyTenant(p.id, this.newReply.trim()).subscribe({
      next: r => {
        this.replies[p.id] = [...(this.replies[p.id] || []), r.data];
        this.newReply = '';
        this.replying = false;
        p.replyCount = (p.replyCount || 0) + 1;
      },
      error: () => this.replying = false
    });
  }

  canDeletePost(p: ForumPost): boolean { return p.author?.id === this.me?.userId; }
  canDeleteReply(r: ForumReply): boolean { return r.author?.id === this.me?.userId; }

  deletePost(p: ForumPost) {
    if (!confirm(`Delete your post "${p.title}"?`)) return;
    this.api.deleteForumPostTenant(p.id).subscribe(() => this.load());
  }

  deleteReply(p: ForumPost, r: ForumReply) {
    if (!confirm('Delete your reply?')) return;
    this.api.deleteForumReplyTenant(r.id).subscribe(() => {
      this.replies[p.id] = (this.replies[p.id] || []).filter(x => x.id !== r.id);
      p.replyCount = Math.max(0, (p.replyCount || 0) - 1);
    });
  }

  authorBadge(p: ForumPost | ForumReply): string {
    return p.author?.role === 'OWNER' ? '👑 Owner' : '👤 Tenant';
  }
}
