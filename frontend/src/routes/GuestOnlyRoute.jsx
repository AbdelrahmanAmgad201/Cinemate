import { Navigate, Outlet } from 'react-router-dom';
import { AuthContext } from "../context/AuthContext.jsx";
import { useContext } from "react";
import LoadingFallback from "../components/LoadingFallback.jsx";
import {PATHS} from "../constants/constants.jsx";

export default function GuestOnlyRoute({ redirectTo = PATHS.HOME }) {

    const { loading, isAuthenticated, pendingRestored } = useContext(AuthContext);

    if (loading || !pendingRestored) {

        return <LoadingFallback />;
    }

    if (isAuthenticated) {
        // if logged in
        return <Navigate to={redirectTo} replace />;
    }


    return <Outlet />;

}