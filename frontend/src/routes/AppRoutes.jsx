import {Routes, Route, Navigate} from 'react-router-dom'

import GuestOnlyRoute from './GuestOnlyRoute'
import PendingUserRoute from "./PendingUserRoute.jsx";
import RoleRoute from "./RoleRoute.jsx";

import UserSignIn from "../pages/auth/SignIn.jsx";
import UserSignUp from "../pages/auth/SignUp.jsx";
import EmailVerification from "../pages/auth/EmailVerification.jsx";
import HomePage from "../pages/user/HomePage.jsx";
import ExploreForums from "../pages/user/ExploreForums.jsx";
import ForumPage from "../pages/user/ForumPage.jsx";
import Browse from "../pages/user/Browse.jsx";
import Genre from '../pages/user/Genre.jsx';
import OrgSignUp from "../pages/org/auth/OrgSignUp.jsx";
import OrgSignIn from "../pages/org/auth/OrgSignIn.jsx";
import AdminSignIn from "../pages/admin/auth/AdminSignIn.jsx";
import OAuthRedirect from "../pages/user/auth/OAuthRedirect.jsx";
import OrgAddMovie from "../pages/org/OrgAddMovie.jsx";
import OrgMoviesAndAnalytics from "../pages/org/OrgMoviesAndAnalytics.jsx";
import MoviePreviewPage from "../pages/user/MoviePreviewPage.jsx";
import WatchPage from "../pages/user/WatchPage.jsx";
import PostFullPage from '../pages/user/PostFullPage.jsx';

import NotFoundPage from "../components/NotFoundPage.jsx";
import TestSandBox from "../pages/TestSandBox.jsx";

import ReviewMovies from "../pages/admin/ReviewMovies.jsx";
import SiteAnalytics from "../pages/admin/SiteAnalytics.jsx";

import { ROLES, PATHS } from "../constants/constants.jsx";
import AddAdmin from "../pages/admin/AddAdmin.jsx";
import UserMainLayout from "../components/UserMainLayout.jsx";
import SimpleLayout from "../components/SimpleLayout.jsx";
import Forum from "../pages/user/Forum.jsx";
import Mod from "../pages/user/Mod.jsx";

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
                <Route path={PATHS.ADMIN.ADD_ADMIN} element={<AddAdmin />} />
                {/*<Route path={PATHS.MOVIE.DETAILS()} element={<MoviePreviewPage />} />*/}
            </Route>

            <Route element={<RoleRoute allowedRoles={[ROLES.USER, ROLES.ADMIN]} />}>
                <Route element={<SimpleLayout />}>
                    {/*<Route path={PATHS.HOME} element={<HomePage />} />*/}
                    {/*<Route path={PATHS.MOVIE.BROWSE} element={<Browse />} />*/}
                    <Route path={PATHS.MOVIE.DETAILS()} element={<MoviePreviewPage />} />
                </Route>
            </Route>

            {/*Added here for testing*/}
            {/*<Route path="/test-sand-box" element={<TestSandBox />} />*/}
            <Route element={<UserMainLayout />}>
                <Route path={PATHS.FORUM.PAGE()} element={<Forum/>} />
            </Route>
            {/* protected routes (requires login + verified) */}
            <Route element={<RoleRoute allowedRoles={[ROLES.USER]} />}>
                <Route element={<UserMainLayout />}> {/* Navbar + Sidebar*/}
                    <Route path={PATHS.HOME} element={<HomePage />} />
                    <Route path={PATHS.FORUM.EXPLORE} element={<ExploreForums />} />
                    <Route path={PATHS.POST.FULLPAGE()} element={<PostFullPage />} />
                    <Route path={PATHS.FORUM.PAGE()} element={<Forum/>} />
                </Route>
                <Route element={<SimpleLayout />}> {/* Navbar only */}
                    <Route path={PATHS.MOVIE.BROWSE} element={<Browse />} />
                    <Route path={PATHS.MOVIE.GENRE()} element={<Genre />} />
                    {/*<Route path={PATHS.MOVIE.DETAILS()} element={<MoviePreviewPage />} />*/}
                    <Route path={PATHS.MOVIE.WATCH} element={<WatchPage />} />
                    <Route path={PATHS.MOD.PAGE()} element={<Mod />}/>
                    
                </Route>                

            </Route>

            {/* If any unknown path is entered, it will be redirected to the UserSignIn page*/}
            <Route path="*" element={<NotFoundPage />} />

        </Routes>
    )
}