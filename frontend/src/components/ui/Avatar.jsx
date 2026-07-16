import PropTypes from 'prop-types';
import './style/Avatar.css';

function initialsFrom(name) {
    if (!name) return '?';
    const parts = name.trim().split(/\s+/);
    const first = parts[0]?.[0] ?? '';
    const second = parts.length > 1 ? parts[parts.length - 1][0] : '';
    return (first + second).toUpperCase();
}

export default function Avatar({ src, name, size = 'md', className = '' }) {
    return (
        <span className={`avatar avatar--${size} ${className}`}>
            {src
                ? <img src={src} alt={name ? `${name}'s avatar` : 'Avatar'} className="avatar__img" />
                : <span aria-hidden="true">{initialsFrom(name)}</span>}
        </span>
    );
}

Avatar.propTypes = {
    src: PropTypes.string,
    name: PropTypes.string,
    size: PropTypes.oneOf(['xs', 'sm', 'md', 'lg', 'xl']),
    className: PropTypes.string,
};
