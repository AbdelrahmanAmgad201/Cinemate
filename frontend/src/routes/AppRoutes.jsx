import {Routes, Route} from 'react-router-dom'
import GuestOnlyRoute from './GuestOnlyRoute'
import ProtectedRoute from "./ProtectedRoute.jsx";

import UserRoutes from './userRoutes';
import OrgRoutes from './orgRoutes';
import AdminRoutes from './adminRoutes';
import UserSignIn from "../pages/user/auth/signIn.jsx";
import UserSignUp from "../pages/user/auth/signUp.jsx";
import EmailVerification from "../pages/user/auth/EmailVerification.jsx";
import HomePage from "../pages/user/homePage.jsx";


export default function AppRoutes() {



    return (
        <Routes>

            {/* public routes, i think the only public pages will be auth pages */}
            <Route element={<GuestOnlyRoute />}>
                {/*The following is equivalent to path = ""*/}
                <Route index element={<UserSignIn />} />
                <Route path={"/userSignUp"} element={<UserSignUp />} />
                <Route path={"/email-verify"} element={<EmailVerification />} />

                {OrgRoutes()}
                {AdminRoutes()}

            </Route>


            {/* protected routes (requires login + verified) */}
            <Route element={<ProtectedRoute requireVerified={true} />}>
                <Route path="/home-page" element={<HomePage />} />
                 {/*other protected routes*/}
            </Route>

            {/*<Route path="*" element={<NotFoundPage />} />*/}

        </Routes>
    )
}