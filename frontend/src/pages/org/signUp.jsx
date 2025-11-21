import React from 'react';
import { useNavigate } from 'react-router-dom';
import SignUp from "../user/auth/signUp.jsx";

const OrgSignUp = () => {
    return (
        <SignUp
            role = "Organization"
            show = {false}
            link = "/orgSignIn"
        />
    );
};

export default OrgSignUp;