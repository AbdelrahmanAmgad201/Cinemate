import PropTypes from 'prop-types';
import Button from './Button.jsx';
import './style/EmptyState.css';

export default function EmptyState({ icon, title, description, actionLabel, onAction, className = '' }) {
    return (
        <div className={`empty-state ${className}`}>
            {icon && <div className="empty-state__icon">{icon}</div>}
            <p className="empty-state__title">{title}</p>
            {description && <p className="empty-state__description">{description}</p>}
            {actionLabel && onAction && (
                <Button variant="secondary" size="sm" onClick={onAction}>{actionLabel}</Button>
            )}
        </div>
    );
}

EmptyState.propTypes = {
    icon: PropTypes.node,
    title: PropTypes.string.isRequired,
    description: PropTypes.string,
    actionLabel: PropTypes.string,
    onAction: PropTypes.func,
    className: PropTypes.string,
};
