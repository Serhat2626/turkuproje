import { useState } from 'react';

interface NoteDialogProps {
  open: boolean;
  title: string;
  label: string;
  placeholder?: string;
  required?: boolean;
  confirmText?: string;
  cancelText?: string;
  variant?: 'danger' | 'primary';
  loading?: boolean;
  onConfirm: (note: string) => void;
  onCancel: () => void;
}

function NoteDialog({
  open,
  title,
  label,
  placeholder = '',
  required = true,
  confirmText = 'Gönder',
  cancelText = 'İptal',
  variant = 'primary',
  loading = false,
  onConfirm,
  onCancel,
}: NoteDialogProps) {
  const [note, setNote] = useState('');

  if (!open) return null;

  function handleConfirm() {
    if (required && !note.trim()) return;
    onConfirm(note);
    setNote('');
  }

  function handleCancel() {
    setNote('');
    onCancel();
  }

  return (
    <div className="dialog-overlay" onClick={handleCancel}>
      <div className="dialog-content" onClick={(e) => e.stopPropagation()}>
        <h3 className="dialog-title">{title}</h3>
        <div className="form-group" style={{ marginBottom: 16 }}>
          <label className="form-label">{label} {required && '*'}</label>
          <textarea
            className="form-textarea"
            value={note}
            onChange={(e) => setNote(e.target.value)}
            placeholder={placeholder}
            rows={4}
          />
          {required && !note.trim() && (
            <span style={{ fontSize: '0.8rem', color: 'var(--accent-red)' }}>Bu alan zorunludur</span>
          )}
        </div>
        <div className="dialog-actions">
          <button type="button" className="btn btn-secondary" onClick={handleCancel} disabled={loading}>
            {cancelText}
          </button>
          <button
            type="button"
            className={`btn ${variant === 'danger' ? 'btn-danger' : 'btn-primary'}`}
            onClick={handleConfirm}
            disabled={loading || (required && !note.trim())}
          >
            {loading && <span className="loading-spinner" />}
            {loading ? 'İşleniyor…' : confirmText}
          </button>
        </div>
      </div>
    </div>
  );
}

export default NoteDialog;
