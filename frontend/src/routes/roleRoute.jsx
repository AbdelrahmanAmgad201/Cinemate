import { Navigate, Outlet } from "react-router-dom";
import { useContext } from "react";
import { AuthContext } from "../context/authContext";
import LoadingFallback from "../components/loadingFallback.jsx";


export default function RoleRoute({ allowedRoles = [], redirectTo = "/" }) {

    const { user, loading, isAuthenticated, pendingUser, pendingRestored } = useContext(AuthContext);

    if (loading || !pendingRestored) {
        return <LoadingFallback />;
    }

    // if user is not authenticated, go to login page
    if (!isAuthenticated || !user) {
        return <Navigate to={redirectTo} replace />;
    }

    if (allowedRoles.length > 0 && !allowedRoles.includes(user.role)) {
        console.log(`User role ${user.role} is not authorized to access this page`);
        // User is logged in but not in the allowed roles -> send them to a suitable page
        // You may choose to send ADMIN -> /review-movies, USER -> /home-page, ORG -> /org-add-movie
        // Here we redirect to a generic home or "not authorized" page.
        if (user.role === "ADMIN") return ( <Navigate to="/review-movies" replace />)
        else if (user.role === "ORGANIZATION") return ( <Navigate to="/org-add-movie" replace />)
        else return <Navigate to="/home-page" replace />;
    }

    return <Outlet />;
}