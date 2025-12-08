import React from 'react';
import SignIn from "../../auth/SignIn.jsx";
import {PATHS} from "../../../constants/constants.jsx";

const AdminSignIn = () => {
    return (
        <SignIn
        role = "Admin"
        button2 = "User"
        navigate2 = {PATHS.ROOT}
        showParagraph = {false}
        />
    );
};

export default AdminSignIn;