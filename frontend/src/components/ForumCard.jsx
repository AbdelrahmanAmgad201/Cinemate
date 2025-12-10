import React, {useState} from 'react';
import { useNavigate } from 'react-router-dom';
import { PATHS } from '../constants/constants.jsx';
import '../components/style/forumCard.css';

export default function ForumCard({forum, onFollowChange}){
    const [followed, setFollowed] = useState(!!forum.followed);
    const navigate = useNavigate();

    function toggleFollow(e){
        e.preventDefault();
        e.stopPropagation();
        const next = !followed;
        setFollowed(next);
        if (onFollowChange) onFollowChange(forum.id, next);
    }

    function handleCardClick() {
        navigate(PATHS.FORUM.DETAILS(forum.id));
    }

    return (
        <div className="forum-card" onClick={handleCardClick} role="button" tabIndex={0} onKeyPress={(e) => e.key === 'Enter' && handleCardClick()}>
            <div className="forum-card-left">
                <div className="forum-avatar" style={{backgroundImage: `url(${forum.avatar || '/public/default-forum.png'})`}} />
                <div className="forum-info">
                    <div className="forum-name">{forum.name}</div>
                    <div className="forum-meta">{(forum.followerCount ?? 0).toLocaleString()} Followers</div>
                    <div className="forum-desc">{forum.description}</div>
                </div>
            </div>

            <div className="forum-card-right">
                <button className={`follow-btn ${followed ? 'followed' : ''}`} onClick={toggleFollow} aria-pressed={followed}>
                    {followed ? 'Following' : 'Follow'}
                </button>
            </div>
        </div>
    )
}
