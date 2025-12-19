import {FiCheck, FiUsers} from 'react-icons/fi';
import './style/JoinButton.css';
import {useEffect, useRef, useState} from "react";
import {useNavigate} from "react-router-dom";


export default function JoinButton({ onJoin }){
    const [isOpen, setIsOpen] = useState(false);
    const [code, setCode] = useState('');
    const containerRef = useRef(null);
    const navigate = useNavigate();

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (containerRef.current && !containerRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    const handleConfirm = () => {
        if (code.trim()) {
            // onJoin(code);
            setIsOpen(false);
            setCode('');
            // TODO

            console.log("Joining room:", code);
            // Redirect the user to the watch party page with the code
            navigate(`/watch-party/${code}`);

        }
    };
    return (
        <div className="join-wrapper" ref={containerRef}>

            <button className="join-btn" onClick={() => setIsOpen(!isOpen)} >
                <FiUsers size={30} />
                <span className="join-text">Join Party</span>
            </button>

            {isOpen && (
                <div className="join-popover" onClick={(e) => e.stopPropagation()}>
                    <input
                        type="text"
                        placeholder="Room Code"
                        value={code}
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