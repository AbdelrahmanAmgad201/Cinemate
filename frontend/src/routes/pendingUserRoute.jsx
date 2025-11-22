import { useContext } from "react";
import { Navigate, Outlet } from "react-router-dom";
import { AuthContext } from "../context/authContext";

export default function PendingUserRoute() {
    const { pendingUser, pendingRestored } = useContext(AuthContext);

    if (!pendingRestored) return null; // don’t redirect until state is restored

    if (!pendingUser) {
        console.log("pendingUser is null, redirecting");
        return <Navigate to="/user-sign-in" replace />;
    }

    return <Outlet />;

}
