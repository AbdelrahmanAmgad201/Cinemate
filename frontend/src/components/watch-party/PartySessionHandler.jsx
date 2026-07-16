import { ArrowRight, Check, LogOut, Users } from 'lucide-react';
import './style/PartySessionHandler.css';
import { useContext, useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { ToastContext } from "../../context/ToastContext.jsx";
import { ROLES } from "../../constants/constants.jsx";
import { Tooltip } from 'react-tooltip';
import { WatchPartyContext } from "../../context/WatchPartyContext.jsx";
import Input from '../ui/Input.jsx';
import IconButton from '../ui/IconButton.jsx';
import ConfirmDialog from '../ui/ConfirmDialog.jsx';

export default function PartySessionHandler(){
    const { activePartyId, role, joinParty, leaveOrEndParty} = useContext(WatchPartyContext);
    const { showToast } = useContext(ToastContext)

    const containerRef = useRef(null);
    const navigate = useNavigate();
    const location = useLocation();

    const [isOpen, setIsOpen] = useState(false);
    const [code, setCode] = useState('');
    const [confirmEndOpen, setConfirmEndOpen] = useState(false);
    const isHost = role === ROLES.WATCH_PARTY_HOST;
    const isInsideActiveRoom = location.pathname === `/watch-party/${activePartyId}`;


    useEffect(() => {
        const handleClickOutside = (event) => {
            if (containerRef.current && !containerRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);


    const handleJoin = async () => {
        if (!code.trim()) return;

        showToast("Watch Party", "Joining party...", "info")

        const res = await joinParty(code, ROLES.WATCH_PARTY_GUEST);

        if (!res.success) {
            showToast("Watch Party", res.message, "error");
            return;
        }

        showToast("Watch Party", "Joined successfully!", "success");

        setIsOpen(false);
        setCode('');
        navigate(`/watch-party/${code}`);
    };

    const handleQuickLeave = (e) => {
        e.stopPropagation();

        if (isHost) {
            setConfirmEndOpen(true);
            return;
        }

        finishLeave();
    };

    const finishLeave = async () => {
        setConfirmEndOpen(false);
        await leaveOrEndParty();

        showToast("Watch Party", isHost ? "Party Ended" : "You left the party", "info");

        // If they were actually in the room page, kick them home
        if (location.pathname.includes(activePartyId)) {
            navigate('/');
        }
    };

    return (
        <div className="party-handler-wrapper" ref={containerRef}>
            {!activePartyId ? (
                /* STATE 1: NO ACTIVE SESSION */
                <button
                    className="handler-btn join-mode"
                    onClick={() => setIsOpen(!isOpen)}
                    data-tooltip-id="handler-tip"
                    data-tooltip-content="Join a watch party"
                >
                    <Users size={22} />
                    <span className="join-text">Join Party</span>
                </button>
            ) : isInsideActiveRoom ? (
                /* STATE 2: CURRENTLY IN THE ROOM (Leave Only) */
                <button
                    className="handler-btn leave-only-mode"
                    onClick={handleQuickLeave}
                    data-tooltip-id="handler-tip"
                    data-tooltip-content={isHost ? "End Party" : "Leave Party"}
                >
                    <LogOut size={22} />
                    <span>{isHost ? "End Party" : "Leave Party"}</span>
                </button>
            ) : (
                /* STATE 3: ACTIVE SESSION BUT ELSEWHERE (Split Button) */
                <div className="split-handler-container">
                    <button
                        className="handler-btn go-to-part"
                        onClick={() => navigate(`/watch-party/${activePartyId}`)}
                        data-tooltip-id="handler-tip"
                        data-tooltip-content="Back to the party"
                    >
                        <ArrowRight size={22} />
                        <span>Go To Party</span>
                    </button>
                    <IconButton
                        variant="ghost"
                        label={isHost ? "End Party" : "Leave Party"}
                        onClick={handleQuickLeave}
                        data-tooltip-id="handler-tip"
                        data-tooltip-content={isHost ? "End Party" : "Leave Party"}
                    >
                        <LogOut size={18} />
                    </IconButton>
                </div>
            )}

            <Tooltip id="handler-tip" />

            {isOpen && (
                <div className="join-popover">
                    <div className="popover-arrow"></div>
                    <Input
                        placeholder="Enter Room Code"
                        value={code}
                        onChange={(e) => setCode(e.target.value)}
                        autoFocus
                        className="join-popover__field"
                    />
                    <IconButton variant="solid" label="Join party" onClick={handleJoin}>
                        <Check size={18} />
                    </IconButton>
                </div>
            )}

            <ConfirmDialog
                open={confirmEndOpen}
                onClose={() => setConfirmEndOpen(false)}
                onConfirm={finishLeave}
                title="Confirm ending party?"
                message="Ending the party will close it for everyone. Continue?"
                confirmLabel="Yes, end"
                danger
            />
        </div>
    );
}
