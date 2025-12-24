import React, { createContext, useState, useContext, useEffect } from 'react';
import { AuthContext } from './AuthContext';
import { ToastContext } from './ToastContext';
import {ROLES, SESSION_STORAGE} from '../constants/constants';
import * as partyApi from '../api/watch-together-api'; // Import all as an object

export const WatchPartyContext = createContext();

export const WatchPartyProvider = ({ children }) => {
    const { user, loading: userLoading } = useContext(AuthContext);
    const [activeParty, setActiveParty] = useState(null);
    const [loading, setLoading] = useState(true);

    const userPartyKey = user ? SESSION_STORAGE.ACTIVE_PARTY_KEY(user.id) : null;

    // Load session on mount
    useEffect(() => {
        if (userLoading) return;

        if (userPartyKey) {
            const saved = sessionStorage.getItem(userPartyKey);
            if (saved) setActiveParty(JSON.parse(saved));
        }
        setLoading(false);
    }, [userPartyKey]);

    // Internal helper to save state
    const updateSession = (partyId, role) => {
        const sessionData = { partyId, role, joinedAt: new Date().toISOString() };
        sessionStorage.setItem(userPartyKey, JSON.stringify(sessionData));
        setActiveParty(sessionData);
    };

    const createParty = async (movieId) => {
        if (activeParty){
            return {success: false, message: "You're already in a party! Please leave it before creating a new one."};
        }

        const res = await partyApi.createRoomApi({ movieId });
        if (!res.success) {
            return res;
        }

        updateSession(res.data.partyId, ROLES.WATCH_PARTY_HOST);
        return res;
    };

    const joinParty = async (partyId) => {
        const res = await partyApi.joinRoomApi({ partyId });
        if (!res.success) {
            return res
        }

        updateSession(partyId, ROLES.WATCH_PARTY_GUEST);
        return res;
    };

    const leaveOrEndParty = async () => {
        if (!activeParty) return;

        const isHost = activeParty.role === ROLES.WATCH_PARTY_HOST;
        const apiCall = isHost ? partyApi.deleteRoomApi : partyApi.leaveRoomApi;

        await apiCall({ partyId: activeParty.partyId });

        // We clear locally regardless of server success to unblock the user
        sessionStorage.removeItem(userPartyKey);
        setActiveParty(null);
    };

    return (
        <WatchPartyContext.Provider value={{
            activeParty,
            activePartyId: activeParty?.partyId,
            role: activeParty?.role,
            createParty,
            joinParty,
            leaveOrEndParty,
            loading
        }}>
            {children}
        </WatchPartyContext.Provider>
    );
};