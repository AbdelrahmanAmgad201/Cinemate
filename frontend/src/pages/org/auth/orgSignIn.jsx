import React from 'react';
import SignIn from "../../auth/signIn.jsx";
import {PATHS, ROLES} from "../../../constants/constants.jsx";

const OrgSignIn = () => {
    return (
        <SignIn
            role = "Organization"
            button1 = "User"
            navigate1 = {PATHS.ROOT}
            link = {PATHS.ORGANIZATION.SIGN_UP}
        />
    );
};

export default OrgSignIn;