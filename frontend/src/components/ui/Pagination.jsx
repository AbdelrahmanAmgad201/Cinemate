import PropTypes from 'prop-types';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import IconButton from './IconButton.jsx';
import './style/Pagination.css';

function getPageWindow(current, total) {
    // Always show first, last, current, and one neighbor either side;
    // collapse the rest into ellipses so wide totals stay compact.
    const pages = new Set([0, total - 1, current, current - 1, current + 1]);
    return [...pages].filter(p => p >= 0 && p < total).sort((a, b) => a - b);
}

export default function Pagination({ page, totalPages, onPageChange, className = '' }) {
    if (totalPages <= 1) return null;

    const window = getPageWindow(page, totalPages);

    return (
        <nav className={`pagination ${className}`} aria-label="Pagination">
            <IconButton
                label="Previous page"
                disabled={page === 0}
                onClick={() => onPageChange(page - 1)}
            >
                <ChevronLeft size={18} />
            </IconButton>

            {window.map((p, i) => (
                <span key={p} style={{ display: 'flex', alignItems: 'center' }}>
                    {i > 0 && window[i - 1] !== p - 1 && <span className="pagination__ellipsis">…</span>}
                    <button
                        type="button"
                        className={`pagination__page ${p === page ? 'pagination__page--active' : ''}`}
                        aria-current={p === page ? 'page' : undefined}
                        onClick={() => onPageChange(p)}
                    >
                        {p + 1}
                    </button>
                </span>
            ))}

            <IconButton
                label="Next page"
                disabled={page === totalPages - 1}
                onClick={() => onPageChange(page + 1)}
            >
                <ChevronRight size={18} />
            </IconButton>
        </nav>
    );
}

Pagination.propTypes = {
    page: PropTypes.number.isRequired,
    totalPages: PropTypes.number.isRequired,
    onPageChange: PropTypes.func.isRequired,
    className: PropTypes.string,
};
