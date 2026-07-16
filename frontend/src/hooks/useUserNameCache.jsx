import { useState, useEffect } from 'react';
import { getModApi } from '../api/forum-api';

// Module-level cache shared by every consumer of this hook, so the same user id is
// only ever looked up once per page load regardless of how many comments render it.
const userNameCache = {};

async function fetchUserNameById(userId) {
    if (!userId) return null;
    if (userNameCache[userId]) return userNameCache[userId];
    try {
        const res = await getModApi({ userId });
        if (!res.success) return null;
        const text = res.data;
        if (text && text !== 'Unknown user' && typeof text === 'string' && !text.trim().toLowerCase().startsWith('<!doctype html>')) {
            userNameCache[userId] = text;
            return text;
        }
        return null;
    } catch (e) {
        console.error('[UserNameLookup] Error for userId', userId, e);
        return null;
    }
}

// Resolves a display name for a user id via the mod-lookup endpoint, cached across
// the app's lifetime. Returns null until (and unless) a name is found.
export function useUserNameCache(userId) {
    const [name, setName] = useState(userId ? userNameCache[userId] || null : null);

    useEffect(() => {
        let ignore = false;
        if (userId) {
            fetchUserNameById(userId).then(resolved => {
                if (!ignore && resolved) setName(resolved);
            });
        }
        return () => { ignore = true; };
    }, [userId]);

    return name;
}
