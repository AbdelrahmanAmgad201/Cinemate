import { Route } from "react-router-dom";
import UserSignIn from "../pages/user/auth/UserSignIn.jsx";
import UserSignUp from "../pages/user/auth/UserSignUp.jsx";
import HomePage from "../pages/user/homePage";
import EmailVerification from "../pages/user/auth/EmailVerification.jsx";

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