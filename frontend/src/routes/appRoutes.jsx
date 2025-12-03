import {Routes, Route, Navigate} from 'react-router-dom'

import GuestOnlyRoute from './GuestOnlyRoute'
import PendingUserRoute from "./PendingUserRoute.jsx";
import RoleRoute from "./roleRoute.jsx";

import UserSignIn from "../pages/auth/signIn.jsx";
import UserSignUp from "../pages/auth/signUp.jsx";
import EmailVerification from "../pages/auth/emailVerification.jsx";
import HomePage from "../pages/user/homePage.jsx";
import Browse from "../pages/user/browse.jsx";
import Genre from '../pages/user/genre.jsx';
import OrgSignUp from "../pages/org/auth/orgSignUp.jsx";
import OrgSignIn from "../pages/org/auth/orgSignIn.jsx";
import AdminSignIn from "../pages/admin/auth/adminSignIn.jsx";
import OAuthRedirect from "../pages/user/auth/OAuthRedirect.jsx";
import OrgAddMovie from "../pages/org/orgAddMovie.jsx";
import OrgMoviesAndAnalytics from "../pages/org/orgMoviesAndAnalytics.jsx";
import MoviePreviewPage from "../pages/user/moviePreviewPage.jsx";
import WatchPage from "../pages/user/watchPage.jsx";

import NotFoundPage from "../components/notFoundPage.jsx";
import TestSandBox from "../pages/testSandBox.jsx";

import ReviewMovies from "../pages/admin/reviewMovies.jsx";
import SiteAnalytics from "../pages/admin/siteAnalytics.jsx";

import { ROLES, PATHS } from "../constants/constants.jsx";

export default function AppRoutes() {



    return (
        <Routes>

            {/* public routes, currently the only public pages will be auth pages */}
            <Route element={<GuestOnlyRoute />}>

                <Route path={PATHS.ROOT}
                       element={ <Navigate to={PATHS.USER.SIGN_IN} replace/> }
                />

                <Route path={PATHS.USER.SIGN_IN} element={<UserSignIn />} />
                <Route path={PATHS.USER.SIGN_UP} element={<UserSignUp />} />
                <Route path={PATHS.GOOGLE_AUTH.REDIRECT} element={<OAuthRedirect />} />

                <Route path={PATHS.ORGANIZATION.SIGN_IN} element={<OrgSignIn />} />
                <Route path={PATHS.ORGANIZATION.SIGN_UP} element={<OrgSignUp />} />

                <Route path={PATHS.ADMIN.SIGN_IN} element={<AdminSignIn />} />

                <Route element={<PendingUserRoute />}>
                    <Route path={PATHS.EMAIL_VERIFICATION} element={<EmailVerification />} />
                </Route>

            </Route>

            <Route element={<RoleRoute allowedRoles={[ROLES.ORGANIZATION]} redirectTo={PATHS.ORGANIZATION.SIGN_IN} />}>
                <Route path={PATHS.ORGANIZATION.SUBMIT_REQUEST} element={<OrgAddMovie />} />
                <Route path={PATHS.ORGANIZATION.MOVIES_ANALYTICS} element={<OrgMoviesAndAnalytics />} />
            </Route>

            <Route element={<RoleRoute allowedRoles={[ROLES.ADMIN]} redirectTo={PATHS.ADMIN.SIGN_IN} />}>
                <Route path={PATHS.ADMIN.REVIEW_REQUESTS} element={<ReviewMovies />} />
                <Route path={PATHS.ADMIN.SITE_ANALYTICS} element={<SiteAnalytics />} />
                {/*<Route path={PATHS.MOVIE.DETAILS()} element={<MoviePreviewPage />} />*/}
            </Route>

            <Route element={<RoleRoute allowedRoles={[ROLES.USER, ROLES.ADMIN]} />}>
                {/*<Route path={PATHS.HOME} element={<HomePage />} />*/}
                {/*<Route path={PATHS.MOVIE.BROWSE} element={<Browse />} />*/}
                <Route path={PATHS.MOVIE.DETAILS()} element={<MoviePreviewPage />} />
            </Route>

            {/*Added here for testing*/}
            {/*<Route path="/test-sand-box" element={<TestSandBox />} />*/}

            {/* protected routes (requires login + verified) */}
            <Route element={<RoleRoute allowedRoles={[ROLES.USER]} />}>
                <Route path={PATHS.HOME} element={<HomePage />} />
                <Route path={PATHS.MOVIE.BROWSE} element={<Browse />} />
                <Route path={PATHS.MOVIE.GENRE()} element={<Genre />} />
                {/*<Route path={PATHS.MOVIE.DETAILS()} element={<MoviePreviewPage />} />*/}
                <Route path={PATHS.MOVIE.WATCH} element={<WatchPage />} />

            </Route>

            {/* If any unknown path is entered, it will be redirected to the UserSignIn page*/}
            <Route path="*" element={<NotFoundPage />} />

        </Routes>
    )
}