import { useLocation, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import WistiaEmbed from '../../components/WistiaEmbed.jsx';
import IconButton from '../../components/ui/IconButton.jsx';
import './style/WatchPage.css';

export default function WatchPage() {
    const location = useLocation();
    const navigate = useNavigate();
    const wistiaId = location.state;

    return (
        <div className="watch-page">
            <IconButton label="Back" size="lg" className="watch-page__back" onClick={() => navigate(-1)}>
                <ArrowLeft size={20} />
            </IconButton>
            <WistiaEmbed wistiaId={wistiaId} className="wistia_full" />
        </div>
    );
}
