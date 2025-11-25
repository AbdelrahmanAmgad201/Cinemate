import { useParams, useLocation } from "react-router-dom";
import WistiaEmbed from "../../components/wistiaEmbed.jsx";
import './style/watchPage.css';

export default function WatchPage() {
    const { videoID } = useParams();

    return (
        <div className="watch-page">
            <WistiaEmbed wistiaId={videoID} className="wistia_full" />
        </div>
    );
}
