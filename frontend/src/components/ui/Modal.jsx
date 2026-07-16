import { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import PropTypes from 'prop-types';
import { X } from 'lucide-react';
import IconButton from './IconButton.jsx';
import './style/Modal.css';

/**
 * Accessible dialog primitive: traps focus, closes on Escape/backdrop click,
 * restores focus to the trigger element on close. Every modal in the app
 * (edit post, confirmations, review composer, etc.) should render through
 * this instead of the old CommonModal.css markup.
 */
export default function Modal({ open, onClose, title, size = 'md', children, footer }) {
    const dialogRef = useRef(null);
    const previouslyFocused = useRef(null);
    const onCloseRef = useRef(onClose);

    useEffect(() => {
        onCloseRef.current = onClose;
    }, [onClose]);

    // Deliberately depends only on `open`, not `onClose`: callers pass an inline
    // onClose that gets a new identity on every render, and this effect re-focuses
    // the first focusable element on setup — including it in the deps stole focus
    // from whatever the user was typing into on every keystroke.
    useEffect(() => {
        if (!open) return;

        previouslyFocused.current = document.activeElement;
        const dialog = dialogRef.current;
        const focusable = dialog?.querySelectorAll(
            'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
        );
        (focusable?.[0] ?? dialog)?.focus();

        const handleKeyDown = (e) => {
            if (e.key === 'Escape') {
                onCloseRef.current();
                return;
            }
            if (e.key !== 'Tab' || !focusable?.length) return;

            const first = focusable[0];
            const last = focusable[focusable.length - 1];
            if (e.shiftKey && document.activeElement === first) {
                e.preventDefault();
                last.focus();
            } else if (!e.shiftKey && document.activeElement === last) {
                e.preventDefault();
                first.focus();
            }
        };

        document.addEventListener('keydown', handleKeyDown);
        document.body.style.overflow = 'hidden';

        return () => {
            document.removeEventListener('keydown', handleKeyDown);
            document.body.style.overflow = '';
            previouslyFocused.current?.focus?.();
        };
    }, [open]);

    if (!open) return null;

    return createPortal(
        <div className="modal-overlay" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}>
            <div
                className={`modal modal--${size}`}
                role="dialog"
                aria-modal="true"
                aria-label={title}
                ref={dialogRef}
                tabIndex={-1}
            >
                {title && (
                    <div className="modal__header">
                        <h2 className="modal__title">{title}</h2>
                        <IconButton label="Close" onClick={onClose}>
                            <X size={18} />
                        </IconButton>
                    </div>
                )}
                <div className="modal__body">{children}</div>
                {footer && <div className="modal__footer">{footer}</div>}
            </div>
        </div>,
        document.body
    );
}

Modal.propTypes = {
    open: PropTypes.bool.isRequired,
    onClose: PropTypes.func.isRequired,
    title: PropTypes.string,
    size: PropTypes.oneOf(['sm', 'md', 'lg']),
    children: PropTypes.node,
    footer: PropTypes.node,
};
