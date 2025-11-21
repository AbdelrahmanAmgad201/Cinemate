import React from 'react';
import { useNavigate } from 'react-router-dom';
import SignIn from "../auth/signIn.jsx";

const OrgSignIn = () => {
    const navigate = useNavigate();
    return (
        <SignIn
            role = "Organization"
            button1 = "User"
            navigate1 = "/"
            link = "/org-sign-up"
        />
    );
};

export default OrgSignIn;