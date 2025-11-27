// ErrorToastContext.js
import React, { createContext, useContext, useState } from "react";
import Alert from "@mui/material/Alert";
import AlertTitle from "@mui/material/AlertTitle";
import IconButton from "@mui/material/IconButton";
import CloseIcon from "@mui/icons-material/Close";
import Collapse from "@mui/material/Collapse";
import { v4 as uuidv4 } from "uuid";

// eslint-disable-next-line react-refresh/only-export-components
export const ErrorToastContext = createContext();

export const ErrorToastProvider = ({ children }) => {
    const [toasts, setToasts] = useState([]);

    const showError = (title = "Error", message) => {
        const id = uuidv4();
        setToasts((prev) => [...prev, { id, title, message }]);
    };

    const removeToast = (id) => {
        setToasts((prev) => prev.filter((t) => t.id !== id));
    };

    return (
        <ErrorToastContext.Provider value={{ showError }}>
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
                            severity="error"
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
        </ErrorToastContext.Provider>
    );
};

// eslint-disable-next-line react-refresh/only-export-components
// export const useErrorToast = () => {
//     return useContext(ErrorToastContext);
// };
