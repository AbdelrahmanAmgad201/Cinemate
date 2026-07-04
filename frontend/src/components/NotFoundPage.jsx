import { useNavigate } from 'react-router-dom';
import { Compass } from 'lucide-react';
import { PATHS } from '../constants/constants.jsx';
import EmptyState from './ui/EmptyState.jsx';
import './style/notFoundPage.css';

export default function NotFoundPage() {
    const navigate = useNavigate();

    return (
        <div className="notfound-container">
            <span className="notfound-code">404</span>
            <EmptyState
                icon={<Compass size={28} />}
                title="Page not found"
                description="The page you're looking for doesn't exist or may have moved."
                actionLabel="Go home"
                onAction={() => navigate(PATHS.HOME)}
            />
        </div>
    );
}
