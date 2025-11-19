import React from 'react';
import { useNavigate } from 'react-router-dom';

const UserSignUp = () => {
    const navigate = useNavigate();
    return (
        <div>
            <h1>
                User Sign Up
            </h1>
            <button type="submit" onClick={() => navigate("/emailVerif")}>Sign Up</button>
        </div>
    );
};

export default UserSignUp;