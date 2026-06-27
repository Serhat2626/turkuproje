export default function LoadingSpinner({ text = 'Yükleniyor...' }) {
  return (
    <div className="spinner-container">
      <div className="spinner"></div>
      <span className="spinner-text">{text}</span>
    </div>
  )
}
