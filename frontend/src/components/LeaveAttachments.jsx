import { useRef, useState } from 'react';
import { apiFetchBlob, apiUpload } from '../api/client';
import { useApi } from '../hooks/useApi';

export default function LeaveAttachments({ leaveRequestId }) {
  const { data: attachments, reload } = useApi(`/api/leave-requests/${leaveRequestId}/attachments`);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const fileInputRef = useRef(null);

  async function handleFileChange(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    setError('');
    setUploading(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      await apiUpload(`/api/leave-requests/${leaveRequestId}/attachments`, formData);
      await reload();
    } catch (err) {
      setError(err.message);
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  }

  async function download(attachment) {
    try {
      const blob = await apiFetchBlob(`/api/leave-requests/${leaveRequestId}/attachments/${attachment.id}/download`);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = attachment.fileName;
      document.body.appendChild(a); a.click(); a.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className="attachments-panel">
      {!attachments?.length && <div className="attachments-empty">No attachments yet</div>}
      {attachments?.map((a) => (
        <button key={a.id} type="button" className="attachment-chip" onClick={() => download(a)}>
          📎 {a.fileName} <span className="size">({Math.max(1, Math.round(a.fileSize / 1024))} KB)</span>
        </button>
      ))}
      <label className="attachment-add">
        {uploading ? 'Uploading…' : '+ Add attachment'}
        <input ref={fileInputRef} type="file" accept=".pdf,.png,.jpg,.jpeg,.doc,.docx" onChange={handleFileChange} disabled={uploading} hidden />
      </label>
      {error && <div className="banner-error show">{error}</div>}
    </div>
  );
}
