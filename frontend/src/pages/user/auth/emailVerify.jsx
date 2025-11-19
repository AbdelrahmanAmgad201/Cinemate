import React from 'react';
import { useNavigate } from 'react-router-dom';

const EmailVerification = () => {
    const navigate = useNavigate();
    return (
        <div>
            <h1>
                Email Verification
            </h1>
            <button type="submit" onClick={() => navigate("/homePage")}>Done</button>
        </div>
    );
};

export default EmailVerification;