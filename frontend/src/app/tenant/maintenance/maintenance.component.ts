import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { MaintenanceRequest } from '../../core/models/models';

@Component({
  selector: 'app-tenant-maintenance',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './maintenance.component.html'
})
export class TenantMaintenanceComponent implements OnInit {
  requests: MaintenanceRequest[] = [];
  loading = true;
  showModal = false;
  saving = false;
  uploading = false;
  form = { title: '', issue: '', priority: 'MEDIUM' };
  imageUrls: string[] = [];
  previewUrls: string[] = [];
  error = '';
  priorities = ['LOW','MEDIUM','HIGH','URGENT'];

  constructor(private api: ApiService) {}
  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.api.getMyMaintenance().subscribe({ next: r => { this.requests = r.data || []; this.loading = false; }, error: () => this.loading = false });
  }

  onFileChange(event: any) {
    const files: FileList = event.target.files;
    if (!files.length) return;
    this.previewUrls = [];
    for (let i = 0; i < files.length; i++) {
      const reader = new FileReader();
      reader.onload = (e: any) => this.previewUrls.push(e.target.result);
      reader.readAsDataURL(files[i]);
    }
    this.uploading = true;
    this.api.uploadFiles(files).subscribe({
      next: r => { this.imageUrls = [...this.imageUrls, ...(r.data || [])]; this.uploading = false; },
      error: () => { this.uploading = false; this.error = 'Upload failed'; }
    });
  }

  removePreview(i: number) { this.previewUrls.splice(i, 1); this.imageUrls.splice(i, 1); }

  submit() {
    if (!this.form.title || !this.form.issue) { this.error = 'Please fill all required fields'; return; }
    this.saving = true; this.error = '';
    this.api.raiseMaintenance({ title: this.form.title, issue: this.form.issue, priority: this.form.priority, imageUrls: this.imageUrls }).subscribe({
      next: () => { this.saving = false; this.showModal = false; this.form = { title:'', issue:'', priority:'MEDIUM' }; this.imageUrls = []; this.previewUrls = []; this.load(); },
      error: () => { this.saving = false; this.error = 'Failed to submit'; }
    });
  }

  priorityClass(p: string) { return { LOW:'badge-gray', MEDIUM:'badge-blue', HIGH:'badge-amber', URGENT:'badge-red' }[p] || 'badge-gray'; }
  statusClass(s: string) { return { PENDING:'badge-amber', ASSIGNED:'badge-blue', IN_PROGRESS:'badge-purple', COMPLETED:'badge-green', CANCELLED:'badge-gray' }[s] || 'badge-gray'; }
}
