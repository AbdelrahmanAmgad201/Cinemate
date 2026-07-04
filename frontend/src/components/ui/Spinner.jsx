import PropTypes from 'prop-types';
import './style/Spinner.css';

export default function Spinner({ size = 24, className = '' }) {
    return (
        <span
            className={`spinner ${className}`}
            style={{ width: size, height: size }}
            role="status"
            aria-label="Loading"
        />
    );
}

Spinner.propTypes = {
    size: PropTypes.number,
    className: PropTypes.string,
};
