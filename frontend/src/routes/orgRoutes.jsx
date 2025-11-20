import { Route } from "react-router-dom";
import OrgSignUp from "../pages/org/signUp";
import OrgSignIn from "../pages/org/signIn";

function OrgRoutes() {
  return (
    <>
        <Route path="/orgSignUp" element={<OrgSignUp />} />
        <Route path="/orgSignIn" element={<OrgSignIn />} />
    </>
  );
}export default OrgRoutes
