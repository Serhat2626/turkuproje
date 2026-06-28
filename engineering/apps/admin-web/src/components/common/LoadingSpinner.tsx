interface LoadingSpinnerProps {
  text?: string;
  size?: 'sm' | 'md' | 'lg';
}

function LoadingSpinner({ text = 'Yükleniyor…', size = 'md' }: LoadingSpinnerProps) {
  const sizeMap = { sm: 14, md: 20, lg: 32 };
  const px = sizeMap[size];

  return (
    <div className="loading-text" style={{ justifyContent: 'center', padding: '16px 0' }}>
      <span
        className="loading-spinner"
        style={{ width: px, height: px }}
      />
      {text && <span>{text}</span>}
    </div>
  );
}

export default LoadingSpinner;
