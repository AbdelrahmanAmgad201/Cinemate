import React from 'react';
import { useNavigate } from 'react-router-dom';

const UserSignIn = () => {
    const navigate = useNavigate();
    return (
        <div>
            <h1>
                User Sign In
            </h1>
            <button type="submit" onClick={() => navigate("/homePage")}>Sign In</button>
            <button type="submit" onClick={() => navigate("/userSignUp")}>No Account?</button>
            <button type="submit" onClick={() => navigate("/orgSignIn")}>Organization?</button>
            <button type="submit" onClick={() => navigate("/adminSignIn")}>Admin?</button>
        </div>
    );
};

export default UserSignIn;