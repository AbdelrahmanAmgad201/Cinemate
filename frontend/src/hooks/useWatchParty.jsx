import {useContext, useEffect, useRef} from 'react';
import {BACKEND_URL, PATHS, WATCH_PARTY, WatchPartyEventType} from '../constants/constants.jsx';
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import {ToastContext} from "../context/ToastContext.jsx";
import {useNavigate} from "react-router-dom";

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
export const useWatchParty = (partyId, userId, userName, isHost, handleMessages) => {
    const playerRef = useRef(null); // We set this ref to the specific video player
    const isInternalAction = useRef(false);
    const stompClientRef = useRef(null);

    const navigate = useNavigate();
    const {showToast} = useContext(ToastContext);

    const broadcastAction = (eventType, payload = {}) => {
        // To prevent infinite loops (person does an action, sent to backend,
        // replied with an action, this reply should NOT call the backend again)
        if (isInternalAction.current) {
            isInternalAction.current = false;
            return;
        }

        if (!stompClientRef.current?.connected) return;

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
        })

        stompClientRef.current.publish({
            destination: `/app/party/${partyId}/chat`,
            body: JSON.stringify({
                userId,
                userName,
                eventType: WatchPartyEventType.CHAT,
                payload: { message: "Hello everyone!" },
                timestamp: new Date().toISOString(),
            }),
        });

    }

    const handleIncomingAction = (data) => {

        if (!playerRef.current) return;

        const eventType = data.eventType;
        const payload = data.payload;

        if ([WatchPartyEventType.PLAY, WatchPartyEventType.PAUSE, WatchPartyEventType.SEEK].includes(eventType)) {
            isInternalAction.current = true;
        }

        switch (eventType) {
            case WatchPartyEventType.PLAY:{
                // The following is to prevent control if user didn't click play for the first time
                if (playerRef.current.isBeforePlay()) return;
                playerRef.current.play()
                break
            }
            case WatchPartyEventType.PAUSE:{
                if (playerRef.current.isBeforePlay()) return;
                playerRef.current.pause();
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
                break;
            }
            case WatchPartyEventType.USER_LEFT:
                showToast("Watch Party", data.payload || `${data.userName} left`, "info");
                break;
            case WatchPartyEventType.PARTY_DELETED:{
                showToast("Party Ended", "The host has closed the room", "warning");
                navigate(PATHS.ROOT, {replace: true});
                break;
            }

            case WatchPartyEventType.CHAT:
                handleMessages(data.payload.message);
                break;

            default:
                break;
        }
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