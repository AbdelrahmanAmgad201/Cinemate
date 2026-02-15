import { useState, useRef, useEffect } from "react";
import { RxPinRight, RxPinBottom, RxPinLeft, RxPinTop } from "react-icons/rx";
import { LuSendHorizontal } from "react-icons/lu";
import { FaCircleArrowDown } from "react-icons/fa6";
import { Tooltip } from 'react-tooltip';
import "./style/LiveChat.css";

export default function LiveChat({ fullScreen = false, messages = [], onSendMessage, currentUserColor = "#52B788" }) {  
    
    const [autoScroll, setAutoScroll] = useState(true);
    const [newMsg, setNewMsg] = useState('');
    const [isMinimized, setIsMinimized] = useState(false);
    const chatContainerRef = useRef(null);

    
    const handleSendMessage = () => {
        if (!newMsg.trim() || !onSendMessage) return;
        onSendMessage(newMsg);
        // console.log("msg sent", newMsg);
        setNewMsg("");
    }

    const handleKeyPress = (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
          e.preventDefault();
          handleSendMessage();
      }
    };

    const handleMinimize = () => {
        setIsMinimized(!isMinimized);
    };

    const handleScroll = () => {
        if (chatContainerRef.current) {
            const { scrollTop, scrollHeight, clientHeight } = chatContainerRef.current;
            const isAtBottom = scrollHeight - scrollTop - clientHeight < 50;
            setAutoScroll(isAtBottom);
        }
    };

    const scrollToBottom = () => {
        setAutoScroll(true);
        if (chatContainerRef.current) {
            chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
        }
    };

    useEffect(() => {
        if (autoScroll && chatContainerRef.current) {
          chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
        }
    }, [messages, autoScroll]);

    if (isMinimized && !fullScreen) {
        return (
            <div className="chat-minimized">
                <RxPinRight className="minimize-icon" onClick={() => setIsMinimized(!isMinimized)} data-tooltip-id="open-chat-tooltip" data-tooltip-content="Open Chat" />
                <Tooltip id="open-chat-tooltip" place="right" />
            </div>
        );
    }

    if (isMinimized && fullScreen) {
        return (
            <div className="full-chat-minimized">
                <RxPinTop className="minimize-icon" onClick={() => setIsMinimized(!isMinimized)} data-tooltip-id="open-chat-tooltip" data-tooltip-content="Open Chat" />
                <Tooltip id="open-chat-tooltip" place="right" />
            </div>
        );
    }
      
    return(
        <div className={fullScreen ? "full-chat-container" : "chat-container"}>
            {!fullScreen && (
                <div className="chat-header">
                    <RxPinLeft className="minimize-icon" onClick={() => setIsMinimized(!isMinimized)} data-tooltip-id="close-chat-tooltip" data-tooltip-content="Minimize Chat" />
                    <Tooltip id="close-chat-tooltip" place="right" />
                    <h2>Live Chat</h2>
                </div>
            )}
            <div className="messages-container">
                <div className="message" ref={chatContainerRef} onScroll={handleScroll}>
                    {messages.map(msg => (
                        msg.type === 'system' ?(
                            <div key={msg.id} className="system-message">
                                <span className="system" style={{color: msg.color || "#ff4444"}}>{msg.sender}: </span>
                                <span className="system-content">{msg.content}</span>
                            </div>
                        ) : (
                            <div key={msg.id} className="user-message">
                                <span className="username" style={{color: msg.color}}>{msg.sender}: </span>
                                <span className="message-content">{msg.content}</span>
                            </div>
                        )
                    ))}
                </div>
                {!autoScroll && (
                    <div className="auto-scroll" onClick={scrollToBottom}>
                        <FaCircleArrowDown />
                    </div>
                )}
            </div>

            
            
            <div className="send-message">
                <div className="input-container">
                    <input type="text" placeholder="Send a message" onKeyPress={handleKeyPress} value={newMsg} onChange={(e) => setNewMsg(e.target.value)} minLength={1} maxLength={100}></input>
                    <LuSendHorizontal onClick={handleSendMessage} disabled={!newMsg.trim()} />
                </div>
                
            {fullScreen && (
                <div>
                    <RxPinBottom className="minimize-icon" onClick={() => setIsMinimized(!isMinimized)} data-tooltip-id="close-chat-tooltip" data-tooltip-content="Minimize Chat" />
                    <Tooltip id="close-chat-tooltip" place="right" />
                </div>
            )}
            </div>
        </div>
    );
};