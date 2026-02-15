import React, { createContext, useState, useCallback, useMemo } from "react";
import Alert from "@mui/material/Alert";
import AlertTitle from "@mui/material/AlertTitle";
import IconButton from "@mui/material/IconButton";
import CloseIcon from "@mui/icons-material/Close";
import Collapse from "@mui/material/Collapse";
import { v4 as uuidv4 } from "uuid";

// eslint-disable-next-line react-refresh/only-export-components
export const ToastContext = createContext();

export const ToastProvider = ({ children }) => {
    const [toasts, setToasts] = useState([]);

    // "success"
    // "error"
    // "warning"
    // "info"

    const removeToast = useCallback((id) => {
        setToasts((prev) => prev.filter((t) => t.id !== id));
    }, []);

    const showToast = useCallback((title = "", message = "", severity = "info", ttl = 5000) => {
        const id = uuidv4();
        setToasts((prev) => [...prev, { id, title, message, severity }]);
        if (ttl){
            setTimeout(() => removeToast(id), ttl);
        }
    }, [removeToast]);

    const contextValue = useMemo(() => ({ showToast }), [showToast]);

    return (
        <ToastContext.Provider value={contextValue}>
            {children}

            {/* Toast container */}
            <div
                style={{
                    position: "fixed",
                    top: 16,
                    right: 16,
                    zIndex: 1400,
                    display: "flex",
                    flexDirection: "column",
                    gap: 8,
                    width: 320,
                }}
            >
                {toasts.map((toast) => (
                    <Collapse key={toast.id} in={true}>
                        <Alert
                            severity={toast.severity}
                            action={
                                <IconButton
                                    aria-label="close"
                                    color="inherit"
                                    size="small"
                                    onClick={() => removeToast(toast.id)}
                                >
                                    <CloseIcon fontSize="small" />
                                </IconButton>
                            }
                        >
                            <AlertTitle>{toast.title}</AlertTitle>
                            {toast.message}
                        </Alert>
                    </Collapse>
                ))}
            </div>
        </ToastContext.Provider>
    );
};