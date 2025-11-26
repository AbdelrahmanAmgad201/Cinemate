import {Navigate, Outlet} from 'react-router-dom'
import { AuthContext } from "../context/AuthContext";
import { useContext } from "react";

import LoadingFallback from "../components/loadingFallback.jsx";
export default function ProtectedRoute(){

    const { user, loading, signIn, signOut, isAuthenticated } = useContext(AuthContext);

    if (loading) {

        return <LoadingFallback />;
    }

    if (!isAuthenticated){
        // Not logged in
        return <Navigate to = "" replace />;

    }

    // renders children
    return <Outlet />;
}