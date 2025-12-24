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
export const useWatchParty = (partyId, userId, userName, isHost) => {
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

        // stompClientRef.current.publish({
        //     destination: `/app/party/${partyId}/chat`,
        //     body: JSON.stringify({
        //         userId,
        //         userName,
        //         eventType: WatchPartyEventType.CHAT,
        //         payload: { message: "Hello everyone!" },
        //         timestamp: new Date().toISOString(),
        //     }),
        // });

    }

    const handleIncomingAction = (data) => {
        console.log("Incoming action:");
        console.log(data);//TODO
        if (!playerRef.current) return;

        isInternalAction.current = true;
        const eventType = data.eventType; //TODO
        const payload = data.payload;

        switch (eventType) {
            case WatchPartyEventType.PLAY:
                playerRef.current.play()
                break
            case WatchPartyEventType.PAUSE:
                playerRef.current.pause();
                break
            case WatchPartyEventType.SEEK:{
                const serverTime = payload.time;
                const myTime = playerRef.current.getCurrentTime();
                // if (Math.abs(myTime - serverTime) > WATCH_PARTY.LAG_THRESHOLD) {
                playerRef.current.seek(serverTime)
                // }
                break
            }
            case WatchPartyEventType.USER_JOINED:{
                showToast("Watch Party", data.payload || `${data.userName} joined`, "info");
                console.log("User joined:", isHost);
                if (isHost) {
                    console.log("Sending seek action to host", playerRef.current.getCurrentTime());
                    broadcastAction(WatchPartyEventType.SEEK, { time: playerRef.current.getCurrentTime() });
                }
                break;
            }
            case WatchPartyEventType.USER_LEFT:
                showToast("Watch Party", data.payload || `${data.userName} left`, "info");
                break;
            case WatchPartyEventType.PARTY_DELETED:
                showToast("Party Ended", "The host has closed the room", "warning");
                navigate(PATHS.ROOT, {replace: true});
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
                console.log("🟢 STOMP connected");
                stompClientRef.current = client;
                stompClientRef.current.debug = (msg) => console.log("STOMP:", msg);

                client.subscribe(`/topic/party/${partyId}`, (message) => {
                    const data = JSON.parse(message.body);
                    console.log("Incoming message from party:", data);
                    handleIncomingAction(data);
                });
            },

            onDisconnect: () => {
                console.error("🔴 STOMP disconnected");
                stompClientRef.current = null;
            },

            onWebSocketClose: (evt) => {
                console.error("🔴 WS CLOSED", evt);
            },

            onWebSocketError: (evt) => {
                console.error("❌ WS ERROR", evt);
            },

            onStompError: (frame) => {
                console.error("❌ STOMP ERROR", frame);
            }
        })

        client.activate();

        return () => {
            client.deactivate();
            stompClientRef.current = null;
        }
    }, [partyId])


    return {playerRef, broadcastAction}
}