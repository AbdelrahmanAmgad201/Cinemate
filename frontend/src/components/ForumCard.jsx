import { useNavigate } from 'react-router-dom';
import { PATHS } from '../constants/constants.jsx';
import Avatar from './ui/Avatar.jsx';
import '../components/style/forumCard.css';

export default function ForumCard({ forum }) {
    const navigate = useNavigate();

    function handleCardClick() {
        navigate(PATHS.FORUM.PAGE(forum.id));
    }

    return (
        <div className="forum-card" onClick={handleCardClick} role="button" tabIndex={0} onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && handleCardClick()}>
            <Avatar name={forum.name} src={forum.avatar} size="lg" />
            <div className="forum-info">
                <div className="forum-name">{forum.name}</div>
                <div className="forum-meta">{(forum.followerCount ?? 0).toLocaleString()} followers</div>
                {forum.description && <div className="forum-desc">{forum.description}</div>}
            </div>
        </div>
    );
}
