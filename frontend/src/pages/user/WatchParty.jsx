import {useLocation} from "react-router-dom";
import "./style/WatchParty.css"

export default function WatchParty() {

    // const location = useLocation();
    // const wistiaId = location.state;

    return (
        <div className="watch-party-container">
            {/*  left part -> video player and maybe some stuff beneath it, so it's better to be scrollable  */}
            <main className="watch-party-main">

            </main>

            {/*  right part -> chat, not scrollable  */}
            <aside className="watch-party-sidebar">

            </aside>

        </div>
    )
}