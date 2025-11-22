import { Navigate, Outlet } from 'react-router-dom';
import { AuthContext } from "../context/authContext";
import { useContext } from "react";

export default function GuestOnlyRoute() {

    const { user, loading, signIn, signOut, isAuthenticated } = useContext(AuthContext);

    if (isAuthenticated) {
        // if logged in
        return <Navigate to="/home-page" replace />;
    }


    return <Outlet />;

}