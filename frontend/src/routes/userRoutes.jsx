import { Route } from "react-router-dom";
import UserSignIn from "../pages/user/signIn";
import UserSignUp from "../pages/user/signUp";
import HomePage from "../pages/user/homePage";
import EmailVerification from "../pages/user/emailVerif";

function UserRoutes() {
  return (
    <>
      <Route path="/" element={<UserSignIn />} />
      <Route path="/userSignUp" element={<UserSignUp />} />
      <Route path="/emailVerif" element={<EmailVerification />} />
      <Route path="/homePage" element={<HomePage />} />
    </>
  );
}
export default UserRoutes