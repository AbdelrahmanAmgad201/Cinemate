import { useParams, useLocation } from "react-router-dom";
import WistiaEmbed from "../../components/wistiaEmbed.jsx";
import './style/watchPage.css';

export default function WatchPage() {
    // const { movieID } = useParams();
    const location = useLocation();
    const wistiaId = location.state;
    return (
        <div className="watch-page">
            <WistiaEmbed wistiaId={wistiaId} className="wistia_full" />
        </div>
    );
}
