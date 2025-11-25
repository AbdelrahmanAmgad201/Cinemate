import {Routes, Route} from 'react-router-dom'
import GuestOnlyRoute from './GuestOnlyRoute'
import ProtectedRoute from "./ProtectedRoute.jsx";
import PendingUserRoute from "./PendingUserRoute.jsx";

import UserSignIn from "../pages/auth/signIn.jsx";
import UserSignUp from "../pages/auth/signUp.jsx";
import EmailVerification from "../pages/auth/emailVerification.jsx";
import HomePage from "../pages/user/homePage.jsx";
import OrgSignUp from "../pages/org/auth/orgSignUp.jsx";
import OrgSignIn from "../pages/org/auth/orgSignIn.jsx";
import AdminSignIn from "../pages/admin/auth/adminSignIn.jsx";
import OAuthRedirect from "../pages/user/auth/OAuthRedirect.jsx";
import MoviePreviewPage from "../pages/user/moviePreviewPage.jsx";
import WatchPage from "../pages/user/watchPage.jsx";


export default function AppRoutes() {



    return (
        <Routes>

            {/* public routes, currently the only public pages will be auth pages */}
            <Route element={<GuestOnlyRoute />}>
                {/*The following is equivalent to path = ""*/}
                <Route index element={<UserSignIn />} />
                <Route path={"/user-sign-up"} element={<UserSignUp />} />
                <Route path={"/oauth2/redirect"} element={<OAuthRedirect />} />

                <Route element={<PendingUserRoute />}>
                    <Route path="/email-verification" element={<EmailVerification />} />
                </Route>
                <Route path="/org-sign-in" element={<OrgSignIn />} />
                <Route path="/org-sign-up" element={<OrgSignUp />} />

                <Route path="/admin-sign-in" element={<AdminSignIn />} />

            </Route>

            {/* TEMPORARY */}
            <Route path="/movie/:movieID" element={<MoviePreviewPage />} />
            <Route path="/watch/:videoID" element={<WatchPage />} />

            {/* protected routes (requires login + verified) */}
            <Route element={<ProtectedRoute requireVerified={true} />}>
                <Route path="/home-page" element={<HomePage />} />

            </Route>

            {/* If any unknown path is entered, it will be redirected to the UserSignIn page*/}
            {/*<Route path="*" element={<NotFoundPage />} />*/}
            <Route path="*" element={<UserSignIn />} />

        </Routes>
    )
}