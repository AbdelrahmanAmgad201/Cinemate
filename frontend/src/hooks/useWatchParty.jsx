import {useContext, useEffect, useRef} from 'react';
import {BACKEND_URL, PATHS, WATCH_PARTY, WatchPartyEventType} from '../constants/constants.jsx';
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import {ToastContext} from "../context/ToastContext.jsx";
import {useNavigate} from "react-router-dom";
import {generateColorFromUserId} from "../utils/generate-color.jsx";
import {getAccessToken} from "../auth/tokenStore.js";

// PartyEventPayload {
//     time?: number;
//     message?: string;
// }
//
// PartyEvent {
//     partyId: string;
//     userId?: number;
//     userName?: string;
//     eventType: WatchPartyEventType;
//     payload?: PartyEventPayload;
//     timestamp: number | string;
// }

// This is a general hook for the watch party socket methods, any specific video player / component should
// adapt/use this hook
export const useWatchParty = (partyId, userId, userName, isHost, onIncomingChat) => {
    const playerRef = useRef(null); // We set this ref to the specific video player
    const isInternalAction = useRef(false);
    const stompClientRef = useRef(null);

    const navigate = useNavigate();
    const {showToast} = useContext(ToastContext);

    const onChatRef = useRef(onIncomingChat);

    useEffect(() => {
        onChatRef.current = onIncomingChat;
    }, [onIncomingChat]);

    const broadcastAction = (eventType, payload = {}) => {
        // To prevent infinite loops (person does an action, sent to backend,
        // replied with an action, this reply should NOT call the backend again)
        if (eventType !== WatchPartyEventType.CHAT &&
            [WatchPartyEventType.PLAY, WatchPartyEventType.PAUSE, WatchPartyEventType.SEEK].includes(eventType)) {
            if (isInternalAction.current) {
                return; // Don't reset here, let handleIncomingAction reset it
            }
        }

        if (!stompClientRef.current?.connected) return;

        if(eventType === WatchPartyEventType.CHAT){
            stompClientRef.current.publish({
                destination: `/app/watch-party/${partyId}/chat`,
                body: JSON.stringify({
                    userId,
                    userName,
                    eventType: WatchPartyEventType.CHAT,
                    payload: payload,
                    timestamp: new Date().toISOString(),
                }),
            });
        }
        else{
            stompClientRef.current.publish({
                destination: `/app/watch-party/${partyId}/control`,
                body: JSON.stringify({
                    userId,
                    userName,
                    partyId,
                    eventType,
                    payload,
                    timestamp: new Date().toISOString(),
                }),
            });
        }
    }

    // Truncates the rendered sender name and strips control/formatting characters
    // (SEC-NEW-05) — `userName` on an incoming WebSocket event is attacker-controlled
    // (no message-level auth on this channel yet, see REL-08), and React's JSX escaping
    // already prevents script injection but not an arbitrarily long or control-character-
    // laden name from breaking the chat UI's layout.
    const MAX_SENDER_NAME_LENGTH = 30;
    const sanitizeSenderName = (name) => {
        if (typeof name !== "string") return "Unknown";
        // eslint-disable-next-line no-control-regex
        const stripped = name.replace(/[\x00-\x1F\x7F]/g, "").trim();
        if (!stripped) return "Unknown";
        return stripped.length > MAX_SENDER_NAME_LENGTH
            ? stripped.slice(0, MAX_SENDER_NAME_LENGTH) + "…"
            : stripped;
    };

    const handleChat = (content, type = "system", senderName= "System", color = null) => {
        if(onChatRef.current){
            onChatRef.current({
                id: Date.now().toString() + Math.random(),
                sender: sanitizeSenderName(senderName),
                type: type,
                content: content,
                timestamp: new Date().toLocaleTimeString('en-US', {hour: '2-digit', minute:'2-digit'}),
                color: color
            });
        }
    }

    const handleIncomingAction = (data) => {
        const eventType = data.eventType;
        const payload = data.payload;

        const userColor = data.userId ? generateColorFromUserId(data.userId) : null;

        if(eventType === WatchPartyEventType.CHAT){
            handleChat(payload?.message || "", "text", data.userName, userColor);
            return;
        }

        if (!playerRef.current) return;

        // if ([WatchPartyEventType.PLAY, WatchPartyEventType.PAUSE, WatchPartyEventType.SEEK].includes(eventType)) {
        //     isInternalAction.current = true;
        // }

        switch (eventType) {
            case WatchPartyEventType.PLAY:{
                // The following is to prevent control if user didn't click play for the first time
                if (playerRef.current.isBeforePlay()) return;
                playerRef.current.play()
                // handleChat(`${data.userName} resumed the video`, "system", "System", "#FF4444");
                break
            }
            case WatchPartyEventType.PAUSE:{
                if (playerRef.current.isBeforePlay()) return;
                playerRef.current.pause();
                // handleChat(`${data.userName} paused the video`, "system", "System", "#FF4444");
                break
            }
            case WatchPartyEventType.SEEK:{
                const serverTime = payload.time;
                const myTime = playerRef.current.getCurrentTime();
                // Only seek if the difference between the server time and our time is greater than the threshold
                if (Math.abs(myTime - serverTime) > WATCH_PARTY.LAG_THRESHOLD) {
                    if (playerRef.current.isBeforePlay()) return;
                    playerRef.current.seek(serverTime)
                }
                break
            }
            case WatchPartyEventType.SYNC_REQUEST:{
                if (isHost && playerRef.current){
                    broadcastAction(WatchPartyEventType.SEEK, { time: playerRef.current.getCurrentTime() });
                }
                break
            }
            case WatchPartyEventType.USER_JOINED:{
                showToast("Watch Party", data.payload || `${data.userName} joined`, "info");
                handleChat(`${data.userName} joined the party`, "system", "System", "#44FF44");
                break;
            }
            case WatchPartyEventType.USER_LEFT:
                showToast("Watch Party", data.payload || `${data.userName} left`, "info");
                handleChat(`${data.userName} left the party`, "system", "System", "#FF4444");
                break;
            case WatchPartyEventType.PARTY_DELETED:
                handleChat("The host has closed the room");
                showToast("Party Ended", "The host has closed the room", "warning");
                navigate(PATHS.ROOT, {replace: true});
                break;
            default:
                break;
        }

        // if ([WatchPartyEventType.PLAY, WatchPartyEventType.PAUSE, WatchPartyEventType.SEEK].includes(eventType)) {
        //     setTimeout(() => {
        //         isInternalAction.current = false;
        //     }, 100);
        // }
    }

    // Websocket connection
    useEffect(() => {
        if (!partyId) return;

        const client = new Client({
            webSocketFactory: () => new SockJS(`${BACKEND_URL.WATCH_PARTY_BASE_URL}/ws`),
            reconnectDelay: 5000,

            // Authenticate the STOMP session (REL-08): the service verifies this token on
            // CONNECT and binds the identity it then stamps on every event. Read fresh on
            // each (re)connect so a token refreshed via the axios flow is picked up.
            beforeConnect: () => {
                const token = getAccessToken();
                client.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {};
            },

            onConnect: () => {
                stompClientRef.current = client;

                client.subscribe(`/topic/watch-party/${partyId}`, (message) => {
                    const data = JSON.parse(message.body);
                    handleIncomingAction(data);
                });
            },


        })

        client.activate();

        return () => {
            client.deactivate();
            stompClientRef.current = null;
        }
    }, [partyId])


    return {playerRef, broadcastAction}
}