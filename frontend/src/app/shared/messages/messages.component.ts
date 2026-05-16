import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { ConversationSummary, DirectMessage, User } from '../../core/models/models';

/**
 * Shared 1-on-1 chat page used by both /owner/messages and /tenant/messages.
 * The backend authorizes the relationship — tenants can only chat with their PG
 * owner, owners can chat with any of their active tenants.
 */
@Component({
  selector: 'app-messages',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './messages.component.html'
})
export class MessagesComponent implements OnInit {
  me = this.auth.getCurrentUser();
  conversations: ConversationSummary[] = [];
  partners: User[] = [];
  selectedPartnerId: number | null = null;
  selectedPartner: { id: number; name: string; email: string; role: string } | null = null;
  messages: DirectMessage[] = [];
  newMessage = '';
  loadingThread = false;
  sending = false;
  error = '';

  // For "New Chat" picker (filters out partners we already have a conversation with)
  showPicker = false;

  constructor(private api: ApiService, private auth: AuthService) {}

  ngOnInit() {
    this.loadConversations();
    this.api.getChatPartners().subscribe(r => this.partners = r.data || []);
  }

  loadConversations() {
    this.api.getConversations().subscribe(r => {
      this.conversations = r.data || [];
      // Auto-open the first conversation
      if (this.selectedPartnerId == null && this.conversations.length > 0) {
        this.openChat(this.conversations[0]);
      }
    });
  }

  openChat(c: ConversationSummary | User | { partnerId?: number; id?: number; partnerName?: string; name?: string; partnerEmail?: string; email?: string; partnerRole?: string; role?: string }) {
    const pid = (c as ConversationSummary).partnerId ?? (c as User).id;
    this.selectedPartnerId = pid;
    this.selectedPartner = {
      id: pid,
      name: (c as ConversationSummary).partnerName ?? (c as User).name,
      email: (c as ConversationSummary).partnerEmail ?? (c as User).email,
      role: (c as ConversationSummary).partnerRole ?? (c as User).role
    };
    this.showPicker = false;
    this.loadThread();
  }

  loadThread() {
    if (!this.selectedPartnerId) return;
    this.loadingThread = true;
    this.api.getChatWith(this.selectedPartnerId).subscribe({
      next: r => {
        this.messages = r.data || [];
        this.loadingThread = false;
        setTimeout(() => this.scrollToBottom(), 50);
        // Refresh conv list so unread count zeroes out
        this.loadConversations();
      },
      error: () => this.loadingThread = false
    });
  }

  send() {
    const txt = this.newMessage.trim();
    if (!txt || !this.selectedPartnerId) return;
    this.sending = true;
    this.error = '';
    this.api.sendMessage(this.selectedPartnerId, txt).subscribe({
      next: () => {
        this.newMessage = '';
        this.sending = false;
        this.loadThread();
      },
      error: (err) => {
        this.sending = false;
        this.error = err.error?.message || 'Failed to send';
      }
    });
  }

  isFromMe(m: DirectMessage): boolean {
    return m.sender?.id === this.me?.userId;
  }

  scrollToBottom() {
    const el = document.querySelector('#chat-scroll');
    if (el) el.scrollTop = el.scrollHeight;
  }

  partnersForPicker(): User[] {
    const have = new Set(this.conversations.map(c => c.partnerId));
    return this.partners.filter(p => !have.has(p.id));
  }
}
