import React, { useEffect, useState } from 'react';
import { AiOutlineArrowUp } from 'react-icons/ai';
import './style/ScrollToTop.css';

const SCROLL_THRESHOLD = 300;
const DEFAULT_RIGHT_OFFSET = 20;
const MIN_RIGHT_OFFSET = 12;
const DESKTOP_BREAKPOINT = 768;
const GAP_PADDING = 12;

export default function ScrollToTop() {
    const [visible, setVisible] = useState(false);
    const [rightOffset, setRightOffset] = useState(DEFAULT_RIGHT_OFFSET);

    useEffect(() => {
        const updateVisibility = () => {
            try {
                setVisible(window.scrollY > SCROLL_THRESHOLD);
            } catch (e) {
                // ignore
            }
        };

        const updateOffset = () => {
            try {
                let offset = DEFAULT_RIGHT_OFFSET;
                const sidebar = document.querySelector('.profile-sidebar');
                if (sidebar && window.innerWidth > DESKTOP_BREAKPOINT) {
                    const rect = sidebar.getBoundingClientRect();
                    const dist = Math.max(0, Math.round(window.innerWidth - rect.left));
                    offset = dist + GAP_PADDING;
                }
                if (window.innerWidth <= DESKTOP_BREAKPOINT) offset = MIN_RIGHT_OFFSET;
                setRightOffset(offset);
            } catch (e) {
                setRightOffset(DEFAULT_RIGHT_OFFSET);
            }
        };

        updateVisibility();
        updateOffset();

        window.addEventListener('scroll', updateVisibility, { passive: true });
        window.addEventListener('resize', () => { updateVisibility(); updateOffset(); });
        window.addEventListener('focus', updateOffset);

        let observer = null;
        const sidebarEl = document.querySelector('.profile-sidebar');
        if (sidebarEl && window.MutationObserver) {
            observer = new MutationObserver(() => updateOffset());
            observer.observe(sidebarEl, { attributes: true, childList: false, subtree: false });
        }

        return () => {
            window.removeEventListener('scroll', updateVisibility);
            window.removeEventListener('resize', () => { updateVisibility(); updateOffset(); });
            window.removeEventListener('focus', updateOffset);
            if (observer) observer.disconnect();
        };
    }, []);

    const handleClick = () => {
        try {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        } catch (e) {
            window.scrollTo(0, 0);
        }
    };

    if (!visible) return null;

    const cssVars = { ['--scroll-top-offset']: `${rightOffset}px` };

    return (
        <button
            type="button"
            className="scroll-top-btn"
            onClick={handleClick}
            aria-label="Scroll to top"
            title="Scroll to top"
            style={cssVars}
        >
            <AiOutlineArrowUp size={20} />
        </button>
    );
}