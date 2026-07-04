import { useState, useRef, useEffect } from 'react';
import { PanelRightOpen, PanelLeftClose, PanelBottomClose, PanelTopClose, ArrowDownCircle, SendHorizontal } from 'lucide-react';
import { Tooltip } from 'react-tooltip';
import './style/LiveChat.css';

export default function LiveChat({ fullScreen = false, messages = [], onSendMessage, currentUserColor = '#52B788' }) {
    const [autoScroll, setAutoScroll] = useState(true);
    const [newMsg, setNewMsg] = useState('');
    const [isMinimized, setIsMinimized] = useState(false);
    const chatContainerRef = useRef(null);

    const handleSendMessage = () => {
        if (!newMsg.trim() || !onSendMessage) return;
        onSendMessage(newMsg);
        setNewMsg('');
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSendMessage();
        }
    };

    const handleScroll = () => {
        if (chatContainerRef.current) {
            const { scrollTop, scrollHeight, clientHeight } = chatContainerRef.current;
            setAutoScroll(scrollHeight - scrollTop - clientHeight < 50);
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
                <PanelRightOpen className="minimize-icon" size={18} onClick={() => setIsMinimized(false)} data-tooltip-id="open-chat-tooltip" data-tooltip-content="Open chat" />
                <Tooltip id="open-chat-tooltip" place="right" />
            </div>
        );
    }

    if (isMinimized && fullScreen) {
        return (
            <div className="full-chat-minimized">
                <PanelTopClose className="minimize-icon" size={18} onClick={() => setIsMinimized(false)} data-tooltip-id="open-chat-tooltip" data-tooltip-content="Open chat" />
                <Tooltip id="open-chat-tooltip" place="right" />
            </div>
        );
    }

    return (
        <div className={fullScreen ? 'full-chat-container' : 'chat-container'}>
            {!fullScreen && (
                <div className="chat-header">
                    <PanelLeftClose className="minimize-icon" size={18} onClick={() => setIsMinimized(true)} data-tooltip-id="close-chat-tooltip" data-tooltip-content="Minimize chat" />
                    <Tooltip id="close-chat-tooltip" place="right" />
                    <h2>Live Chat</h2>
                </div>
            )}
            <div className="messages-container">
                <div className="message" ref={chatContainerRef} onScroll={handleScroll}>
                    {messages.map((msg) => (
                        msg.type === 'system' ? (
                            <div key={msg.id} className="system-message">
                                <span className="system" style={{ color: msg.color || 'var(--color-error)' }}>{msg.sender}: </span>
                                <span className="system-content">{msg.content}</span>
                            </div>
                        ) : (
                            <div key={msg.id} className="user-message">
                                <span className="username" style={{ color: msg.color }}>{msg.sender}: </span>
                                <span className="message-content">{msg.content}</span>
                            </div>
                        )
                    ))}
                </div>
                {!autoScroll && (
                    <button type="button" className="auto-scroll" onClick={scrollToBottom} aria-label="Scroll to latest messages">
                        <ArrowDownCircle size={22} />
                    </button>
                )}
            </div>

            <div className="send-message">
                <div className="input-container">
                    <span className="input-container__color-dot" style={{ backgroundColor: currentUserColor }} aria-hidden="true" title="Your chat color" />
                    <input
                        type="text"
                        placeholder="Send a message"
                        onKeyDown={handleKeyPress}
                        value={newMsg}
                        onChange={(e) => setNewMsg(e.target.value)}
                        minLength={1}
                        maxLength={100}
                    />
                    <button type="button" onClick={handleSendMessage} disabled={!newMsg.trim()} aria-label="Send message">
                        <SendHorizontal size={18} />
                    </button>
                </div>

                {fullScreen && (
                    <div>
                        <PanelBottomClose className="minimize-icon" size={18} onClick={() => setIsMinimized(true)} data-tooltip-id="close-chat-tooltip" data-tooltip-content="Minimize chat" />
                        <Tooltip id="close-chat-tooltip" place="right" />
                    </div>
                )}
            </div>
        </div>
    );
}
