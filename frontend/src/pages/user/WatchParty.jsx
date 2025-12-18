import {useLocation, useParams} from "react-router-dom";
import "./style/WatchParty.css"

export default function WatchParty() {

    const { roomId } = useParams();
    // const location = useLocation();
    // const wistiaId = location.state;

    return (
        <div className="watch-party-container">
            {/*  left part -> video player and maybe some stuff beneath it, so it's better to be scrollable  */}
            <main className="watch-party-main">
                {/* Components */}
            </main>

            {/*  right part -> chat, not scrollable  */}
            <aside className="watch-party-sidebar">
                {/* Components */}
            </aside>

        </div>
    )
}