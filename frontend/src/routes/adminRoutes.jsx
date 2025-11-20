import { Route } from "react-router-dom";
import AdminSignIn from "../pages/admin/signIn";

function AdminRoutes() {
  return (
    <>
        <Route path="/adminSignIn" element={<AdminSignIn />} />
    </>
  );
}
export default AdminRoutes