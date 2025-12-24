import {useLocation, useNavigate, useParams} from "react-router-dom";
import "./style/WatchParty.css"
import {useContext, useEffect, useState} from "react";
import {WatchPartyContext} from "../../context/WatchPartyContext.jsx";
import {PATHS, ROLES} from "../../constants/constants.jsx";
import {ToastContext} from "../../context/ToastContext.jsx";
import {getRoomApi} from "../../api/watch-together-api.jsx";

export default function WatchParty() {
    const { activeParty, activePartyId, role, joinParty, leaveOrEndParty, loading} = useContext(WatchPartyContext);
    const { showToast } = useContext(ToastContext)

    const { roomId } = useParams();
    const navigate = useNavigate();

    const [roomLoading, setRoomLoading] = useState(false);
    const [roomData, setRoomData] = useState(null); // Room data, look how the data looks in the API response

    useEffect(() => {

        if (loading) return; // Wait till all states in the context are initialized

        const initializeRoom = async () => {
            setRoomLoading(true);

            // Auto join if user goes to link directly
            if (activePartyId !== roomId) {
                showToast("Watch Party", "Joining party...", "info")

                const res = await joinParty(roomId);

                if (!res.success) {
                    showToast("Watch Party", res.message, "error");
                    navigate(PATHS.ROOT);
                    return null;
                }

                showToast("Watch Party", "Joined successfully!", "success");
            }

            // Fetch room data
            const res = await getRoomApi({ partyId: roomId });
            if (res.success) {
                setRoomData(res.data);
            } else {
                showToast("Watch Party", "Room no longer exists", "error");
                leaveOrEndParty();
                navigate(PATHS.ROOT);
            }

            setRoomLoading(false);
        }

        initializeRoom();
    }, [roomId, loading])

    if (roomLoading || loading) {
        return <div className="loading-container">Connecting to Party...</div>;
    }

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