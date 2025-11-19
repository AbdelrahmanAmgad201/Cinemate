import {Navigate, Outlet} from 'react-router-dom'

export default function ProtectedRoute(){

    const user = null;

    if (!user){
        // Not logged in
        return <Navigate to = "" replace />;

    }

    if (!user.verified){
        return <Navigate to = "signup" replace />;
    }

    // renders children
    return <Outlet />;
}