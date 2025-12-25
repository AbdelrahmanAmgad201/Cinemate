import {useContext, useEffect, useRef} from 'react';
import {BACKEND_URL, PATHS, WATCH_PARTY, WatchPartyEventType} from '../constants/constants.jsx';
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import {ToastContext} from "../context/ToastContext.jsx";
import {useNavigate} from "react-router-dom";
import {generateColorFromUserId} from "../utils/generate-color.jsx";

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
                destination: `/app/party/${partyId}/chat`,
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
                destination: `/app/party/${partyId}/control`,
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

    const handleChat = (content, type = "system", senderName= "System", color = null) => {
        if(onChatRef.current){
            onChatRef.current({
                id: Date.now().toString() + Math.random(),
                sender: senderName,
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

            onConnect: () => {
                stompClientRef.current = client;

                client.subscribe(`/topic/party/${partyId}`, (message) => {
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