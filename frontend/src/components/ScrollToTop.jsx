import React, { useEffect, useState } from 'react';
import { AiOutlineArrowUp } from 'react-icons/ai';
import './style/ScrollToTop.css';

export default function ScrollToTop() {
    const [visible, setVisible] = useState(false);
    const [rightOffset, setRightOffset] = useState(20);

    useEffect(() => {
        const updateVisibility = () => {
            try {
                setVisible(window.scrollY > 300);
            } catch (e) {
                // ignore
            }
        };

        const updateOffset = () => {
            try {
                let offset = 20;
                const sidebar = document.querySelector('.profile-sidebar');
                if (sidebar && window.innerWidth > 768) {
                    const rect = sidebar.getBoundingClientRect();
                    const dist = Math.max(0, Math.round(window.innerWidth - rect.left));
                    offset = dist + 12;
                }
                if (window.innerWidth <= 768) offset = 12;
                setRightOffset(offset);
            } catch (e) {
                setRightOffset(20);
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

    return (
        <button
            type="button"
            className="scroll-top-btn"
            onClick={handleClick}
            aria-label="Scroll to top"
            title="Scroll to top"
            style={{ right: `${rightOffset}px` }}
        >
            <AiOutlineArrowUp size={20} />
        </button>
    );
}