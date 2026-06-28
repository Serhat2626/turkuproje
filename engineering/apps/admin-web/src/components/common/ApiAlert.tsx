interface ApiAlertProps {
  type: 'success' | 'error' | 'info';
  message: string;
  onClose?: () => void;
}

function ApiAlert({ type, message, onClose }: ApiAlertProps) {
  const icons = { success: '✓', error: '✕', info: 'ℹ' };
  return (
    <div className={`api-alert api-alert-${type}`}>
      <span className="api-alert-icon">{icons[type]}</span>
      <span className="api-alert-message">{message}</span>
      {onClose && (
        <button className="api-alert-close" onClick={onClose} type="button">
          ×
        </button>
      )}
    </div>
  );
}

export default ApiAlert;
