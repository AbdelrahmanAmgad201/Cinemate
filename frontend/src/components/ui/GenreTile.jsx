import PropTypes from 'prop-types';
import './style/GenreTile.css';

/**
 * Genre browsing tile — a flat color instead of a poster image, since genres
 * aren't movies and shouldn't borrow a random film's poster to represent
 * themselves.
 */
export default function GenreTile({ label, color, onClick }) {
    return (
        <button type="button" className="genre-tile" style={{ '--genre-color': color }} onClick={onClick}>
            {label}
        </button>
    );
}

GenreTile.propTypes = {
    label: PropTypes.string.isRequired,
    color: PropTypes.string.isRequired,
    onClick: PropTypes.func,
};
