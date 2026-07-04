import PropTypes from 'prop-types';
import './style/Skeleton.css';

/**
 * Shimmering placeholder shown while real data loads. Compose these to
 * mirror the shape of the content that will replace them (see MovieCard's
 * `MovieCardSkeleton` for an example) so the layout doesn't jump on load.
 */
export default function Skeleton({ variant = 'text', width, height, className = '', style = {} }) {
    return (
        <span
            className={`skeleton skeleton--${variant} ${className}`}
            style={{ width, height, ...style }}
            aria-hidden="true"
        />
    );
}

Skeleton.propTypes = {
    variant: PropTypes.oneOf(['text', 'circle', 'rect']),
    width: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    height: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    className: PropTypes.string,
    style: PropTypes.object,
};
