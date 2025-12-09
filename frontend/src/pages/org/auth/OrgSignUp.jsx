import React from 'react';
import SignUp from "../../auth/SignUp.jsx";
import {PATHS, ROLES} from "../../../constants/constants.jsx"

const OrgSignUp = () => {
    return (
        <SignUp
            role = "Organization"
            show = {false}
            link = {PATHS.ORGANIZATION.SIGN_IN}
        />
    );
};

export default OrgSignUp;