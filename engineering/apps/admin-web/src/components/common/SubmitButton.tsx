interface SubmitButtonProps {
  loading: boolean;
  text?: string;
  loadingText?: string;
}

function SubmitButton({
  loading,
  text = 'Kaydet',
  loadingText = 'Kaydediliyor…',
}: SubmitButtonProps) {
  return (
    <button
      type="submit"
      className="btn btn-primary btn-submit"
      disabled={loading}
    >
      {loading && <span className="loading-spinner" />}
      {loading ? loadingText : text}
    </button>
  );
}

export default SubmitButton;
