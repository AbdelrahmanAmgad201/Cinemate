import React from 'react';
import SignIn from "../auth/signIn.jsx";

const AdminSignIn = () => {
    return (
        <SignIn
        role = "Admin"
        button2 = "User"
        navigate2 = "/"
        showParagraph = {false}
        />
    );
};

export default AdminSignIn;