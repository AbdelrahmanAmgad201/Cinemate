import { Navigate, Outlet } from 'react-router-dom';

export default function GuestOnlyRoute() {

    const user = null;

    if (user) {
        // if logged in
        return <Navigate to="/movies" replace />;
    }


    return <Outlet />;

}