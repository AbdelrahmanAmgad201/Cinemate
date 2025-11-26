import { Navigate, Outlet } from 'react-router-dom';
import { AuthContext } from "../context/authContext";
import { useContext } from "react";
import LoadingFallback from "../components/loadingFallback.jsx";

export default function GuestOnlyRoute() {

    const { user, loading, signIn, signOut, isAuthenticated } = useContext(AuthContext);

    if (loading) {

        return <LoadingFallback />;
    }

    if (isAuthenticated) {
        // if logged in
        return <Navigate to="/home-page" replace />;
    }


    return <Outlet />;

}