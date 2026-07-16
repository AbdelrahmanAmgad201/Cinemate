import PropTypes from 'prop-types';
import { Film } from 'lucide-react';
import './style/AuthLayout.css';

/**
 * Shared shell for every auth screen (sign in, sign up, email verification,
 * profile completion) — a branded panel on the left, the form in a card on
 * the right. Replaces the previous "form floating directly on a background
 * image" layout, which made inputs hard to read against the busy poster
 * collage.
 */
export default function AuthLayout({ children, cardWidth = 440 }) {
    return (
        <div className="auth-layout">
            <div className="auth-layout__brand">
                <div className="auth-layout__brand-overlay" />
                <div className="auth-layout__brand-content">
                    <div className="auth-layout__logo">
                        <Film size={26} aria-hidden="true" />
                        <span>Cinemate</span>
                    </div>
                    <p className="auth-layout__tagline">
                        Watch together, in sync, wherever you are — then talk about it in a forum built for it.
                    </p>
                </div>
            </div>

            <div className="auth-layout__panel">
                <div className="auth-layout__card" style={{ maxWidth: cardWidth }}>
                    {children}
                </div>
            </div>
        </div>
    );
}

AuthLayout.propTypes = {
    children: PropTypes.node,
    cardWidth: PropTypes.number,
};
