import PropTypes from 'prop-types';
import Spinner from './ui/Spinner.jsx';
import './style/LoadingFallback.css';

// Shown while something heavy (a route, a data fetch) takes time to load.
// fullScreen centers it in the viewport (route guards, Suspense); the
// default just centers within whatever container it's dropped into
// (e.g. an inline "loading more posts" spot in a feed).
export default function LoadingFallback({ fullScreen = false }) {
    return (
        <div className={`loading-fallback ${fullScreen ? 'loading-fallback--full-screen' : ''}`}>
            <Spinner size={28} />
        </div>
    );
}

LoadingFallback.propTypes = {
    fullScreen: PropTypes.bool,
};
