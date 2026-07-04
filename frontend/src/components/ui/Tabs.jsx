import { useRef } from 'react';
import PropTypes from 'prop-types';
import './style/Tabs.css';

/**
 * Accessible tab list (role=tablist/tab, arrow-key navigation). Consolidates
 * the tab UIs previously hand-rolled in UserProfile and ReviewMovies.
 */
export default function Tabs({ tabs, activeId, onChange, className = '' }) {
    const tabRefs = useRef([]);

    const handleKeyDown = (e, index) => {
        let nextIndex = null;
        if (e.key === 'ArrowRight') nextIndex = (index + 1) % tabs.length;
        if (e.key === 'ArrowLeft') nextIndex = (index - 1 + tabs.length) % tabs.length;
        if (nextIndex === null) return;

        e.preventDefault();
        onChange(tabs[nextIndex].id);
        tabRefs.current[nextIndex]?.focus();
    };

    return (
        <div className={`tabs ${className}`} role="tablist">
            {tabs.map((tab, index) => (
                <button
                    key={tab.id}
                    ref={(el) => (tabRefs.current[index] = el)}
                    role="tab"
                    type="button"
                    aria-selected={tab.id === activeId}
                    tabIndex={tab.id === activeId ? 0 : -1}
                    className={`tabs__tab ${tab.id === activeId ? 'tabs__tab--active' : ''}`}
                    onClick={() => onChange(tab.id)}
                    onKeyDown={(e) => handleKeyDown(e, index)}
                >
                    {tab.icon}
                    {tab.label}
                    {typeof tab.count === 'number' && <span className="tabs__count">{tab.count}</span>}
                </button>
            ))}
        </div>
    );
}

Tabs.propTypes = {
    tabs: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.string.isRequired,
        label: PropTypes.string.isRequired,
        icon: PropTypes.node,
        count: PropTypes.number,
    })).isRequired,
    activeId: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    className: PropTypes.string,
};
