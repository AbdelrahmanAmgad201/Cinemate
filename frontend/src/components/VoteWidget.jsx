import { useState, useEffect } from 'react';
import { BiUpvote, BiDownvote, BiSolidUpvote, BiSolidDownvote } from "react-icons/bi";
import { createVote, updateVote, deleteVote, isVoted } from '../api/vote-api';

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
        setVoteCount(v => v + diff);

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
                // revert
                setUserVote(previousVote);
                setVoteCount(v => v - diff);
                console.error('Vote failed:', result?.message);
            } else {
                // notify parent with previous and new vote so parent can update optimistically
                onChange && onChange({ targetId, previousVote, newVote });
            }
        } catch (e) {
            setUserVote(previousVote);
            setVoteCount(v => v - diff);
            console.error('Vote error:', e);
        }
    };

    return (
        <div className="up-down-vote">
            {userVote === 1 ? (
                <BiSolidUpvote className="selected" onClick={() => handleVote(1)} />
            ) : (
                <BiUpvote onClick={() => handleVote(1)} />
            )}
            <span className="vote-count">{voteCount}</span>
            {isPost && <span className="vote-separator" aria-hidden />}
            {userVote === -1 ? (
                <BiSolidDownvote className="selected" onClick={() => handleVote(-1)} />
            ) : (
                <BiDownvote onClick={() => handleVote(-1)} />
            )}
        </div>
    );
};

export default VoteWidget;
