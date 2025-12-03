import { useContext } from "react";
import { Navigate, Outlet } from "react-router-dom";
import { AuthContext } from "../context/AuthContext.jsx";
import LoadingFallback from "../components/LoadingFallback.jsx";
import {PATHS} from "../constants/constants.jsx";

export default function PendingUserRoute() {
    const { loading, pendingUser, pendingRestored } = useContext(AuthContext);

    if (loading || !pendingRestored) {

        return <LoadingFallback />;
    }

    if (!pendingUser) {
        console.log("pendingUser is null, redirecting to sign in");
        return <Navigate to={PATHS.ROOT} replace />;
    }

    return <Outlet />;

}
