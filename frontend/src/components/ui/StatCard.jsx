import PropTypes from 'prop-types';
import Card from './Card.jsx';
import './style/StatCard.css';

export default function StatCard({ icon, label, value, trend }) {
    return (
        <Card className="stat-card" padding="lg">
            {icon && <span className="stat-card__icon">{icon}</span>}
            <span className="stat-card__value">{value}</span>
            <span className="stat-card__label">{label}</span>
            {trend && <span className={`stat-card__trend stat-card__trend--${trend.direction}`}>{trend.label}</span>}
        </Card>
    );
}

StatCard.propTypes = {
    icon: PropTypes.node,
    label: PropTypes.string.isRequired,
    value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
    trend: PropTypes.shape({
        direction: PropTypes.oneOf(['up', 'down', 'neutral']),
        label: PropTypes.string,
    }),
};
