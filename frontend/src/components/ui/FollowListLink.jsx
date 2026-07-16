import { Link } from 'react-router-dom';
import PropTypes from 'prop-types';
import { PATHS } from '../../constants/constants.jsx';
import Avatar from './Avatar.jsx';

/** A single row in a followers/following list: avatar + name + optional action (e.g. follow button). */
export default function FollowListLink({ userId, username, avatar, action }) {
    return (
        <div className="follow-list-item">
            <Link to={PATHS.USER.PROFILE(userId)} className="follow-list-item__link">
                <Avatar name={username} src={avatar} size="md" />
            </Link>
            <div className="follow-list-item__meta">
                <Link to={PATHS.USER.PROFILE(userId)} className="follow-list-item__name-link">
                    <span className="follow-list-item__name">{username}</span>
                </Link>
            </div>
            {action && <div className="follow-list-item__action">{action}</div>}
        </div>
    );
}

FollowListLink.propTypes = {
    userId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
    username: PropTypes.string.isRequired,
    avatar: PropTypes.string,
    action: PropTypes.node,
};
