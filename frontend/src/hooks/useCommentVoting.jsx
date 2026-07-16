import { useCallback } from 'react';

const cacheKey = (commentId) => `CINEMATE_LAST_COMMENT_${commentId}`;

// Reads back the short-lived vote-count cache written by useCommentVoting, so a
// refresh shows the latest client-side counts until the backend value catches up.
export function readCachedCommentVote(commentId) {
    try {
        return JSON.parse(sessionStorage.getItem(cacheKey(commentId)) || 'null');
    } catch {
        return null;
    }
}

function applyVoteDelta(comment, payload) {
    const prev = payload.previousVote || 0;
    const next = payload.newVote || 0;
    const upDelta = (next === 1 ? 1 : 0) - (prev === 1 ? 1 : 0);
    const downDelta = (next === -1 ? 1 : 0) - (prev === -1 ? 1 : 0);
    return {
        ...comment,
        upvoteCount: (comment.upvoteCount || 0) + upDelta,
        downvoteCount: (comment.downvoteCount || 0) + downDelta,
    };
}

// Applies a VoteWidget change payload to the matching comment in a flat list and
// persists the new counts via the session cache above.
export function useCommentVoting(setComments) {
    return useCallback((payload) => {
        if (!payload) return;
        setComments(prevComments => {
            const updated = prevComments.map(c => (c.id === payload.targetId ? applyVoteDelta(c, payload) : c));
            const target = updated.find(x => x.id === payload.targetId);
            if (target) {
                try {
                    sessionStorage.setItem(cacheKey(target.id), JSON.stringify({
                        upvoteCount: target.upvoteCount,
                        downvoteCount: target.downvoteCount,
                        ts: Date.now(),
                    }));
                } catch { /* ignore storage errors */ }
            }
            return updated;
        });
    }, [setComments]);
}
