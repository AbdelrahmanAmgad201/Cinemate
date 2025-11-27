import { Navigate, Outlet } from 'react-router-dom';
import { AuthContext } from "../context/authContext";
import { useContext } from "react";
import LoadingFallback from "../components/loadingFallback.jsx";

export default function GuestOnlyRoute({ redirectTo = "/home-page" }) {

    const { user, loading, signIn, signOut, isAuthenticated, pendingRestored } = useContext(AuthContext);

    if (loading || !pendingRestored) {

        return <LoadingFallback />;
    }

    if (isAuthenticated) {
        // if logged in
        return <Navigate to={redirectTo} replace />;
    }


    return <Outlet />;

}