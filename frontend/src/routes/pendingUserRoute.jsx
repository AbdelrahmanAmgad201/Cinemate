import { useContext } from "react";
import { Navigate, Outlet } from "react-router-dom";
import { AuthContext } from "../context/authContext";
import LoadingFallback from "../components/loadingFallback.jsx";

export default function PendingUserRoute() {
    const { loading, pendingUser, pendingRestored } = useContext(AuthContext);

    if (loading || !pendingRestored) {

        return <LoadingFallback />;
    }

    if (!pendingUser) {
        console.log("pendingUser is null, redirecting to sign in");
        return <Navigate to="/user-sign-in" replace />;
    }

    return <Outlet />;

}
