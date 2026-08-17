import { useNavigate } from 'react-router-dom';
import { IconArrowLeft } from './icons';

export default function BackButton({ to = '/overview' }) {
  const navigate = useNavigate();
  return (
    <button type="button" className="page-back" onClick={() => navigate(to)} aria-label="Back to overview">
      <IconArrowLeft />
    </button>
  );
}
