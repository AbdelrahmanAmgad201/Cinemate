import PropTypes from 'prop-types';
import './style/Badge.css';

export default function Badge({ variant = 'neutral', size = 'md', className = '', children }) {
    return <span className={`badge badge--${variant} badge--${size} ${className}`}>{children}</span>;
}

Badge.propTypes = {
    variant: PropTypes.oneOf(['neutral', 'accent', 'success', 'error', 'warning']),
    size: PropTypes.oneOf(['sm', 'md']),
    className: PropTypes.string,
    children: PropTypes.node,
};
