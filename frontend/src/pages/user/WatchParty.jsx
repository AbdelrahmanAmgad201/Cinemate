import {useLocation, useNavigate, useParams} from "react-router-dom";
import "./style/WatchParty.css"
import {useCallback, useContext, useEffect, useRef, useState} from "react";
import {WatchPartyContext} from "../../context/WatchPartyContext.jsx";
import {PATHS, ROLES, WATCH_PARTY, WatchPartyEventType} from "../../constants/constants.jsx";
import {ToastContext} from "../../context/ToastContext.jsx";
import {getRoomApi} from "../../api/watch-together-api.jsx";
import WistiaEmbed from "../../components/WistiaEmbed.jsx";
import {useWatchParty} from "../../hooks/useWatchParty.jsx";
import {createWistiaAdapter} from "../../utils/video-player-adapters.jsx";
import {AuthContext} from "../../context/AuthContext.jsx";
import {getModApi} from "../../api/forum-api.jsx";
import LiveChat from "../../components/watch-party/LiveChat.jsx";
import {generateColorFromUserId} from "../../utils/generate-color.jsx";

export default function WatchParty() {
    const { activeParty, activePartyId, role, joinParty, leaveOrEndParty, loading} = useContext(WatchPartyContext);
    const { showToast } = useContext(ToastContext)
    const { user, loading: authLoading } = useContext(AuthContext);
    const [userId, setUserId] = useState(user?.id || null);
    const [userName, setUserName] = useState( "");
    const [userColor, setUserColor] = useState("");
    const isHost = role === ROLES.WATCH_PARTY_HOST;

    const { roomId } = useParams();
    const navigate = useNavigate();

    const [roomLoading, setRoomLoading] = useState(false);
    const [roomData, setRoomData] = useState(null); // Room data, look how the data looks in the API response
    const [messages, setMessages] = useState([]);


    const handleChatMessage = useCallback((message) => {
        setMessages(prevMessages => [...prevMessages, message]);
    }, []);

    const {playerRef, broadcastAction} = useWatchParty(activePartyId, userId, userName, isHost, handleChatMessage);

    const onChatMessage = (messageContent) => {
        if(!messageContent.trim()) return;
        broadcastAction(WatchPartyEventType.CHAT, { message: messageContent });
    };


    const handleOnReady = (video) => {

        const adapter = createWistiaAdapter(video, broadcastAction);
        adapter.init()
        playerRef.current = adapter; // Connect the player to the adapter

        if (!isHost && !adapter.isBeforePlay()) {
            broadcastAction(WatchPartyEventType.SYNC_REQUEST);
            broadcastAction(WatchPartyEventType.PLAY)
        }

    }

    useEffect(() => {

        if (loading || authLoading) return; // Wait till all states in the context are initialized

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

            // Fetch user data
            setUserId(user.id);

            const hexId = user.id.toString(16).padStart(24, '0');
            const response = await getModApi({ userId:hexId });
            const userName = response.data;
            setUserName(userName);
            setUserColor(generateColorFromUserId(user.id));

            setRoomLoading(false);
        }

        initializeRoom();
    }, [roomId, loading])

    const syncInterval = useRef(null);
    const [syncTime, setSyncTime] = useState(3000); // So if can be changed from settings

    // Auto Sync every
    useEffect(() => {
        if (!isHost || !playerRef.current) return;

        // Send host time every syncTime seconds
        syncInterval.current = setInterval(() => {

            if (playerRef.current.isPaused() || playerRef.current.isBeforePlay()) return;

            const currentTime = playerRef.current.getCurrentTime();
            broadcastAction(WatchPartyEventType.SEEK, { time: currentTime });
        }, syncTime);

        return () => clearInterval(syncInterval.current);
    }, [isHost, playerRef, broadcastAction]);

    if (roomLoading || loading) {
        return <div className="loading-container">Connecting to Party...</div>;
    }

    return (
        <div className="watch-party-container">
            {/*  left part -> video player and maybe some stuff beneath it, so it's better to be scrollable  */}
            <main className="watch-party-main">
                {/* Components */}
                <div className="watch-party-video-player" >
                    <WistiaEmbed className="wistia_full" wistiaId={roomData?.movieUrl} onReady={handleOnReady}/>
                </div>
            </main>

            {/*  right part -> chat, not scrollable  */}
            <aside className="watch-party-sidebar">
                <LiveChat messages={messages} onSendMessage={onChatMessage} currentUserColor={userColor} />
            </aside>
        </div>
    )
}