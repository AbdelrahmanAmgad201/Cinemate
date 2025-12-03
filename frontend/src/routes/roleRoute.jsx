import { Navigate, Outlet } from "react-router-dom";
import { useContext } from "react";
import { AuthContext } from "../context/authContext";
import LoadingFallback from "../components/loadingFallback.jsx";
import { ROLES, PATHS} from "../constants/constants.jsx";

export default function RoleRoute({ allowedRoles = [], redirectTo = PATHS.ROOT }) {

    const { user, loading, isAuthenticated, pendingRestored } = useContext(AuthContext);

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
        if (user.role === ROLES.ADMIN) return ( <Navigate to={PATHS.ADMIN.REVIEW_REQUESTS} replace />)
        else if (user.role === ROLES.ORGANIZATION) return ( <Navigate to={PATHS.ORGANIZATION.SUBMIT_REQUEST} replace />)
        else return <Navigate to={PATHS.HOME} replace />;
    }

    return <Outlet />;
}