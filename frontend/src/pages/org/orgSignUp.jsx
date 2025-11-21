import React from 'react';
import { useNavigate } from 'react-router-dom';
import SignUp from "../auth/signUp.jsx";

const OrgSignUp = () => {
    return (
        <SignUp
            role = "Organization"
            show = {false}
            link = "/org-sign-in"
        />
    );
};

export default OrgSignUp;