import {FiCheck, FiUsers} from 'react-icons/fi';
import './style/JoinButton.css';
import {useContext, useEffect, useRef, useState} from "react";
import {useNavigate} from "react-router-dom";
import {ToastContext} from "../../context/ToastContext.jsx";
import {MAX_LENGTHS} from "../../constants/constants.jsx";
import {joinRoomApi} from "../../api/watch-together-api.jsx";
import { Tooltip } from 'react-tooltip';

export default function JoinButton(){
    const [isOpen, setIsOpen] = useState(false);
    const [code, setCode] = useState('');
    const containerRef = useRef(null);
    const navigate = useNavigate();
    const { showToast } = useContext(ToastContext)


    useEffect(() => {
        const handleClickOutside = (event) => {
            if (containerRef.current && !containerRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    const handleConfirm = async () => {
        if (code.trim()) {
            // onJoin(code);
            setIsOpen(false);
            setCode('');
            showToast("Watch Party", "Joining party...", "info")
            // TODO
            const res = await joinRoomApi({code});
            if (!res.success){
                showToast("Watch Party", res.message, "error")
                return;
            }

            showToast("Watch Party", "Joined party!", "success")
            navigate(`/watch-party/${code}`);
        }
    };
    return (
        <div className="join-wrapper" ref={containerRef}>

            <button className="join-btn"
                    onClick={() => setIsOpen(!isOpen)}
                    data-tooltip-id="join-tooltip"
                    data-tooltip-content="Join a watch party with friends!"
                    data-tooltip-place="bottom"
            >
                <FiUsers size={30} />
                <span className="join-text">Join Party</span>
            </button>

            <Tooltip
                id="join-tooltip"
                className="custom-tooltip"
            />

            {isOpen && (
                <div className="join-popover" onClick={(e) => e.stopPropagation()}>
                    <input
                        type="text"
                        placeholder="Room Code"
                        value={code}
                        maxLength={MAX_LENGTHS.INPUT}
                        onChange={(e) => setCode(e.target.value)}
                        autoFocus
                    />
                    <button className="confirm-icon-btn" onClick={handleConfirm}>
                        <FiCheck size={20} />
                    </button>
                    <div className="popover-arrow"></div>
                </div>
            )}
        </div>
    )
};