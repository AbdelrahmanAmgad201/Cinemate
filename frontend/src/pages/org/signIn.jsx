import React from 'react';
import { useNavigate } from 'react-router-dom';

const OrgSignIn = () => {
    const navigate = useNavigate();
    return (
        <div>
            <h1>
                Organization Sign In
            </h1>
            <button type="submit" onClick={() => navigate("/homePage")}>Sign In</button>
            <button type="submit" onClick={() => navigate("/orgSignUp")}>No Account?</button>
        </div>
    );
};

export default OrgSignIn;