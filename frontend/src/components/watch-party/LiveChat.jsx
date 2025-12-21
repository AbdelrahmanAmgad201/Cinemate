import { useState, useRef, useEffect } from "react";
import { RxPinRight, RxPinBottom, RxPinLeft, RxPinTop } from "react-icons/rx";
import { LuSendHorizontal } from "react-icons/lu";
import { FaCircleArrowDown } from "react-icons/fa6";
import { Tooltip } from 'react-tooltip';
import "./style/LiveChat.css";

export default function LiveChat({ fullScreen = false }){
    
    const [autoScroll, setAutoScroll] = useState(true);
    const [newMsg, setNewMsg] = useState('');
    const [isMinimized, setIsMinimized] = useState(false);
    const chatContainerRef = useRef(null);

    const [messages, setMessages] = useState([
        {
          id: "1",
          sender: "NightOwl",
          type: "text",
          content: "This scene is lowkey emotional 😭",
          timestamp: "19:03",
          color: "#52B788"
        },
        {
          id: "2",
          sender: "MovieBuff",
          type: "text",
          content: "Wait till the plot twist 😏",
          spoiler: true,
          timestamp: "19:03",
          color: "#BB8FCE"
        },
        {
          id: "3",
          sender: "System",
          type: "system",
          content: "Host paused the movie",
          timestamp: "19:04",
          color: "#85C1E2"
        },
        {
          id: "4",
          sender: "SpoilerFree",
          type: "text",
          content: "Why is everyone quiet?",
          timestamp: "19:04",
          color: "#F8B739"
        },
        {
            id: "5",
            sender: "NightOwl",
            type: "text",
            content: "This scene is lowkey emotional 😭",
            timestamp: "19:03",
          color: "#52B788"
          },
          {
            id: "6",
            sender: "MovieBuff",
            type: "text",
            content: "Wait till the plot twist 😏",
            spoiler: true,
            timestamp: "19:03",
            color: "#BB8FCE"
          },
          {
            id: "7",
            sender: "System",
            type: "system",
            content: "Host paused the movie",
            timestamp: "19:04",
            color: "#85C1E2"
          },
          {
            id: "8",
            sender: "SpoilerFree",
            type: "text",
            content: "Why is everyone quiet?",
            timestamp: "19:04",
            color: "#F8B739"
          },
          {
            id: "9",
            sender: "NightOwl",
            type: "text",
            content: "This scene is lowkey emotional 😭",
            timestamp: "19:03",
          color: "#52B788"
          },
          {
            id: "10",
            sender: "MovieBuff",
            type: "text",
            content: "Wait till the plot twist 😏",
            spoiler: true,
            timestamp: "19:03",
            color: "#BB8FCE"
          },
          {
            id: "11",
            sender: "System",
            type: "system",
            content: "Host paused the movie",
            timestamp: "19:04",
            color: "#85C1E2"
          },
          {
            id: "12",
            sender: "SpoilerFree",
            type: "text",
            content: "Why is everyone quiet?",
            timestamp: "19:04",
            color: "#F8B739"
          },
          {
            id: "13",
            sender: "NightOwl",
            type: "text",
            content: "This scene is lowkey emotional 😭",
            timestamp: "19:03",
          color: "#52B788"
          },
          {
            id: "14",
            sender: "MovieBuff",
            type: "text",
            content: "Wait till the plot twist 😏",
            spoiler: true,
            timestamp: "19:03",
            color: "#BB8FCE"
          },
          {
            id: "15",
            sender: "System",
            type: "system",
            content: "Host paused the movie",
            timestamp: "19:04",
            color: "#85C1E2"
          },
          {
            id: "16",
            sender: "SpoilerFree",
            type: "text",
            content: "Why is everyone quiet?",
            timestamp: "19:04",
            color: "#F8B739"
          },
          {
            id: "17",
            sender: "NightOwl",
            type: "text",
            content: "This scene is lowkey emotional 😭",
            timestamp: "19:03",
          color: "#52B788"
          },
          {
            id: "18",
            sender: "MovieBuff",
            type: "text",
            content: "Wait till the plot twist 😏",
            spoiler: true,
            timestamp: "19:03",
            color: "#BB8FCE"
          },
          {
            id: "19",
            sender: "System",
            type: "system",
            content: "Host paused the movie",
            timestamp: "19:04",
            color: "#85C1E2"
          },
          {
            id: "20",
            sender: "SpoilerFree",
            type: "text",
            content: "Why is everyone quiet?",
            timestamp: "19:04",
            color: "#F8B739"
          }
      ]);

    const handleSendMessage = () => {
        // TODO
        console.log("msg sent", newMsg);
        setNewMsg("");
    }

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
                                <span className="system">{msg.sender}: </span>
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
                    <input type="text" placeholder="Send a message" value={newMsg} onChange={(e) => setNewMsg(e.target.value)} minLength={1} maxLength={100}></input>
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

// USAGE 
// <div style={{height: "500px", display: "flex"}}>
//     <div style={{width: "900px", height: "500px", backgroundColor: "black"}}></div>
//     <LiveChat />
// </div>

// IN FULL SCREEN
/* .parent-container { 
    position: relative;
    width: 100%;
    height: 100%;
} */

// <div className="parent-container">
//     <video />
//     <LiveChat fullScreen={true} />
// </div> 