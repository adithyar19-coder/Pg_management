import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { Complaint } from '../../core/models/models';

@Component({
  selector: 'app-tenant-complaints',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './complaints.component.html'
})
export class TenantComplaintsComponent implements OnInit {
  complaints: Complaint[] = [];
  loading = true;
  showModal = false;
  saving = false;
  uploading = false;
  form = { title: '', description: '' };
  imageUrls: string[] = [];
  previewUrls: string[] = [];
  error = '';

  constructor(private api: ApiService) {}
  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.api.getMyComplaints().subscribe({ next: r => { this.complaints = r.data || []; this.loading = false; }, error: () => this.loading = false });
  }

  onFileChange(event: any) {
    const files: FileList = event.target.files;
    if (!files.length) return;
    // Show local previews
    this.previewUrls = [];
    for (let i = 0; i < files.length; i++) {
      const reader = new FileReader();
      reader.onload = (e: any) => this.previewUrls.push(e.target.result);
      reader.readAsDataURL(files[i]);
    }
    // Upload
    this.uploading = true;
    this.api.uploadFiles(files).subscribe({
      next: r => { this.imageUrls = [...this.imageUrls, ...(r.data || [])]; this.uploading = false; },
      error: () => { this.uploading = false; this.error = 'Image upload failed'; }
    });
  }

  removePreview(i: number) { this.previewUrls.splice(i, 1); this.imageUrls.splice(i, 1); }

  submit() {
    if (!this.form.title || !this.form.description) { this.error = 'Please fill title and description'; return; }
    this.saving = true; this.error = '';
    this.api.raiseComplaint({ title: this.form.title, description: this.form.description, imageUrls: this.imageUrls }).subscribe({
      next: () => { this.saving = false; this.showModal = false; this.form = { title:'', description:'' }; this.imageUrls = []; this.previewUrls = []; this.load(); },
      error: () => { this.saving = false; this.error = 'Failed to submit complaint'; }
    });
  }

  badgeClass(s: string) { return { OPEN:'badge-red', IN_PROGRESS:'badge-amber', RESOLVED:'badge-green', REJECTED:'badge-gray' }[s] || 'badge-gray'; }
  statusIcon(s: string) { return { OPEN:'🔴', IN_PROGRESS:'🟡', RESOLVED:'🟢', REJECTED:'⚫' }[s] || ''; }
}
