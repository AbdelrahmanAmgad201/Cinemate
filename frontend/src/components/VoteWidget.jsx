import { useState, useEffect } from 'react';
import { ArrowBigUp, ArrowBigDown } from 'lucide-react';
import { createVote, updateVote, deleteVote, isVoted } from '../api/vote-api';
import './style/VoteWidget.css';

const VoteWidget = ({ targetId, initialUp = 0, initialDown = 0, isPost = false, onChange }) => {
    const [userVote, setUserVote] = useState(0);
    const [voteCount, setVoteCount] = useState(initialUp - initialDown);

    useEffect(() => {
        const check = async () => {
            try {
                const res = await isVoted({ targetId });
                if (res.success) {
                    setUserVote(typeof res.data === 'number' ? res.data : 0);
                }
            } catch (e) {
                console.error('Vote check error', e);
            }
        };
        check();
    }, [targetId]);

    // keep vote count in sync when parent updates initial up/down counts
    useEffect(() => {
        setVoteCount((initialUp || 0) - (initialDown || 0));
    }, [initialUp, initialDown]);

    const handleVote = async (voteType) => {
        const previousVote = userVote;
        const newVote = userVote === voteType ? 0 : voteType;
        const diff = newVote - previousVote;

        setUserVote(newVote);
        setVoteCount((v) => v + diff);

        try {
            let result;
            if (previousVote === 0 && newVote !== 0) {
                result = await createVote({ targetId, value: newVote, isPost });
            } else if (previousVote !== 0 && newVote === 0) {
                result = await deleteVote({ targetId });
            } else if (previousVote !== 0 && newVote !== 0) {
                result = await updateVote({ targetId, value: newVote });
            }

            if (!result?.success) {
                setUserVote(previousVote);
                setVoteCount((v) => v - diff);
                console.error('Vote failed:', result?.message);
            } else {
                onChange && onChange({ targetId, previousVote, newVote });

                try {
                    const prevUp = previousVote === 1 ? 1 : 0;
                    const prevDown = previousVote === -1 ? 1 : 0;
                    const newUp = newVote === 1 ? 1 : 0;
                    const newDown = newVote === -1 ? 1 : 0;
                    const upDelta = newUp - prevUp;
                    const downDelta = newDown - prevDown;
                    const key = `CINEMATE_LAST_COMMENT_${targetId}`;
                    const existing = JSON.parse(sessionStorage.getItem(key) || 'null') || {};
                    const baseUp = (typeof existing.upvoteCount === 'number') ? existing.upvoteCount : (initialUp || 0);
                    const baseDown = (typeof existing.downvoteCount === 'number') ? existing.downvoteCount : (initialDown || 0);
                    const updated = { upvoteCount: baseUp + upDelta, downvoteCount: baseDown + downDelta, ts: Date.now() };
                    sessionStorage.setItem(key, JSON.stringify(updated));
                } catch {
                    /* ignore storage errors */
                }
            }
        } catch (e) {
            setUserVote(previousVote);
            setVoteCount((v) => v - diff);
            console.error('Vote error:', e);
        }
    };

    return (
        <div className="vote-widget">
            <button type="button" className={`vote-widget__btn ${userVote === 1 ? 'vote-widget__btn--up-active' : ''}`} onClick={() => handleVote(1)} aria-label="Upvote" aria-pressed={userVote === 1}>
                <ArrowBigUp size={18} fill={userVote === 1 ? 'currentColor' : 'none'} />
            </button>
            <span className="vote-widget__count">{voteCount}</span>
            <button type="button" className={`vote-widget__btn ${userVote === -1 ? 'vote-widget__btn--down-active' : ''}`} onClick={() => handleVote(-1)} aria-label="Downvote" aria-pressed={userVote === -1}>
                <ArrowBigDown size={18} fill={userVote === -1 ? 'currentColor' : 'none'} />
            </button>
        </div>
    );
};

export default VoteWidget;
