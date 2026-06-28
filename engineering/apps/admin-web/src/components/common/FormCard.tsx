import type { ReactNode } from 'react';

interface FormCardProps {
  icon: string;
  title: string;
  subtitle?: string;
  children: ReactNode;
}

function FormCard({ icon, title, subtitle, children }: FormCardProps) {
  return (
    <div className="form-card">
      <div className="form-card-header">
        <span className="form-card-icon">{icon}</span>
        <div>
          <h2 className="form-card-title">{title}</h2>
          {subtitle && <p className="form-card-subtitle">{subtitle}</p>}
        </div>
      </div>
      <div className="form-card-body">{children}</div>
    </div>
  );
}

export default FormCard;
