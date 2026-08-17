import { useEffect, useRef, useState } from 'react';
import { apiFetch, apiUpload } from '../api/client';
import { useApi } from '../hooks/useApi';
import { useAuth } from '../context/AuthContext';
import { roleLabel } from '../utils/roles';
import AvatarCircle from '../components/AvatarCircle';

function initialsOf(name) {
  return (name || '').split(' ').filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('');
}

export default function ProfilePage() {
  const { data: profile, reload } = useApi('/api/profile');
  const { refreshUser } = useAuth();
  const [form, setForm] = useState(null);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');
  const [saveSuccess, setSaveSuccess] = useState('');

  const [pwForm, setPwForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [pwSubmitting, setPwSubmitting] = useState(false);
  const [pwError, setPwError] = useState('');
  const [pwSuccess, setPwSuccess] = useState('');

  const [photoUploading, setPhotoUploading] = useState(false);
  const [photoError, setPhotoError] = useState('');
  const photoInputRef = useRef(null);

  useEffect(() => {
    if (profile) {
      setForm({
        phone: profile.phone || '',
        dateOfBirth: profile.dateOfBirth || '',
        bloodGroup: profile.bloodGroup || '',
        emergencyContactName: profile.emergencyContactName || '',
        emergencyContactRelationship: profile.emergencyContactRelationship || '',
        emergencyContactPhone: profile.emergencyContactPhone || '',
      });
    }
  }, [profile]);

  function setField(key, value) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  async function handleSave(e) {
    e.preventDefault();
    setSaveError('');
    setSaveSuccess('');
    setSaving(true);
    try {
      await apiFetch('/api/profile', {
        method: 'PATCH',
        body: JSON.stringify({ ...form, dateOfBirth: form.dateOfBirth || null }),
      });
      setSaveSuccess('Profile updated.');
      await reload();
    } catch (err) {
      setSaveError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleChangePassword(e) {
    e.preventDefault();
    setPwError('');
    setPwSuccess('');
    if (pwForm.newPassword !== pwForm.confirmPassword) {
      setPwError('New password and confirmation do not match');
      return;
    }
    setPwSubmitting(true);
    try {
      await apiFetch('/api/profile/change-password', {
        method: 'POST',
        body: JSON.stringify({ currentPassword: pwForm.currentPassword, newPassword: pwForm.newPassword }),
      });
      setPwSuccess('Password updated.');
      setPwForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      setPwError(err.message);
    } finally {
      setPwSubmitting(false);
    }
  }

  async function handlePhotoChange(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    setPhotoError('');
    setPhotoUploading(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      await apiUpload('/api/profile/photo', formData);
      await reload();
      await refreshUser();
    } catch (err) {
      setPhotoError(err.message);
    } finally {
      setPhotoUploading(false);
      if (photoInputRef.current) photoInputRef.current.value = '';
    }
  }

  async function handleRemovePhoto() {
    setPhotoError('');
    setPhotoUploading(true);
    try {
      await apiFetch('/api/profile/photo', { method: 'DELETE' });
      await reload();
      await refreshUser();
    } catch (err) {
      setPhotoError(err.message);
    } finally {
      setPhotoUploading(false);
    }
  }

  if (!profile || !form) {
    return <section><div className="page-head"><h1>My Profile</h1></div></section>;
  }

  return (
    <section>
      <div className="page-head">
        <h1>My Profile</h1>
      </div>

      <div className="profile-header">
        <AvatarCircle photoUrl={profile.avatarPhotoDataUri} initials={initialsOf(profile.fullName)} />
        <div>
          <div className="name">{profile.fullName}</div>
          <div className="sub">{profile.jobTitle || roleLabel(profile.role)} · {profile.email}</div>
        </div>
      </div>

      <div className="profile-grid">
        <div>
          <div className="panel">
            <div className="panel-head"><h2>Personal Info</h2></div>
            <div className="profile-panel-body">
              <div className="photo-upload-row">
                <AvatarCircle photoUrl={profile.avatarPhotoDataUri} initials={initialsOf(profile.fullName)} className="photo-upload-avatar" />
                <div>
                  <label className="file-picker-label">
                    {photoUploading ? 'Uploading…' : (profile.avatarPhotoDataUri ? 'Change photo' : 'Upload photo')}
                    <input ref={photoInputRef} type="file" accept="image/png,image/jpeg,image/webp" onChange={handlePhotoChange} disabled={photoUploading} hidden />
                  </label>
                  {profile.avatarPhotoDataUri && (
                    <button type="button" className="photo-remove-link" onClick={handleRemovePhoto} disabled={photoUploading}>Remove photo</button>
                  )}
                  <div className="photo-upload-hint">PNG, JPEG or WEBP, up to 2MB</div>
                </div>
              </div>
              {photoError && <div className="banner-error show">{photoError}</div>}

              <div className={'banner-error' + (saveError ? ' show' : '')}>{saveError}</div>
              <div className={'banner-success' + (saveSuccess ? ' show' : '')}>{saveSuccess}</div>
              <form onSubmit={handleSave}>
                <div className="profile-field-row">
                  <div className="form-field">
                    <label>Full name</label>
                    <input value={profile.fullName} disabled />
                  </div>
                  <div className="form-field">
                    <label>Email</label>
                    <input value={profile.email} disabled />
                  </div>
                </div>
                <div className="profile-field-row">
                  <div className="form-field">
                    <label>Phone</label>
                    <input value={form.phone} onChange={(e) => setField('phone', e.target.value)} placeholder="Phone number" />
                  </div>
                  <div className="form-field">
                    <label>Date of birth</label>
                    <input type="date" value={form.dateOfBirth} onChange={(e) => setField('dateOfBirth', e.target.value)} />
                  </div>
                </div>
                <div className="profile-field-row single">
                  <div className="form-field">
                    <label>Blood group</label>
                    <select value={form.bloodGroup} onChange={(e) => setField('bloodGroup', e.target.value)}>
                      <option value="">Select…</option>
                      {['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'].map((bg) => (
                        <option key={bg} value={bg}>{bg}</option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="profile-section-divider">
                  <h3>Emergency Contact</h3>
                  <div className="profile-field-row">
                    <div className="form-field">
                      <label>Contact name</label>
                      <input value={form.emergencyContactName} onChange={(e) => setField('emergencyContactName', e.target.value)} placeholder="Full name" />
                    </div>
                    <div className="form-field">
                      <label>Relationship</label>
                      <input value={form.emergencyContactRelationship} onChange={(e) => setField('emergencyContactRelationship', e.target.value)} placeholder="e.g. Spouse, Parent" />
                    </div>
                  </div>
                  <div className="profile-field-row single">
                    <div className="form-field">
                      <label>Contact phone</label>
                      <input value={form.emergencyContactPhone} onChange={(e) => setField('emergencyContactPhone', e.target.value)} placeholder="Phone number" />
                    </div>
                  </div>
                </div>

                <button type="submit" className="btn-submit" disabled={saving} style={{ marginTop: 8 }}>
                  {saving ? 'Saving…' : 'Save changes'}
                </button>
              </form>

              <div className="profile-section-divider">
                <h3>Change Password</h3>
                <div className={'banner-error' + (pwError ? ' show' : '')}>{pwError}</div>
                <div className={'banner-success' + (pwSuccess ? ' show' : '')}>{pwSuccess}</div>
                <form onSubmit={handleChangePassword}>
                  <div className="profile-field-row single">
                    <div className="form-field">
                      <label>Current password</label>
                      <input type="password" required value={pwForm.currentPassword}
                        onChange={(e) => setPwForm((f) => ({ ...f, currentPassword: e.target.value }))} />
                    </div>
                  </div>
                  <div className="profile-field-row">
                    <div className="form-field">
                      <label>New password</label>
                      <input type="password" required minLength={8} value={pwForm.newPassword}
                        onChange={(e) => setPwForm((f) => ({ ...f, newPassword: e.target.value }))} />
                    </div>
                    <div className="form-field">
                      <label>Confirm new password</label>
                      <input type="password" required minLength={8} value={pwForm.confirmPassword}
                        onChange={(e) => setPwForm((f) => ({ ...f, confirmPassword: e.target.value }))} />
                    </div>
                  </div>
                  <button type="submit" className="btn-submit" disabled={pwSubmitting}>
                    {pwSubmitting ? 'Updating…' : 'Update password'}
                  </button>
                </form>
              </div>
            </div>
          </div>
        </div>

        <div>
          <div className="panel">
            <div className="panel-head"><h2>Employment</h2></div>
            <div className="profile-panel-body">
              <div className="profile-readonly-row"><span className="label">Employee code</span><span className="value">{profile.employeeCode || '—'}</span></div>
              <div className="profile-readonly-row"><span className="label">Job title</span><span className="value">{profile.jobTitle || '—'}</span></div>
              <div className="profile-readonly-row"><span className="label">Department</span><span className="value">{profile.departmentName || '—'}</span></div>
              <div className="profile-readonly-row"><span className="label">Manager</span><span className="value">{profile.managerName || '—'}</span></div>
              <div className="profile-readonly-row"><span className="label">Role</span><span className="value">{roleLabel(profile.role)}</span></div>
              <div className="profile-readonly-row"><span className="label">Hire date</span><span className="value">{profile.hireDate || '—'}</span></div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
