import { Film } from 'lucide-react';
import './style/Footer.css';

const Footer = () => {
    return (
        <footer className="app-footer">
            <div className="app-footer__inner">
                <div className="app-footer__brand">
                    <Film size={18} aria-hidden="true" />
                    <span>Cinemate</span>
                </div>
                <p className="app-footer__tagline">Watch together. Talk about it after.</p>
            </div>
        </footer>
    );
};

export default Footer;
