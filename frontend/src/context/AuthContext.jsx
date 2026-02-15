import {useState, useEffect, createContext, useContext} from 'react'
import signInApi from '../api/sign-in-api.jsx';
import signOutApi from '../api/sign-out-api.jsx';
import signUpApi from '../api/sign-up-api.jsx';
import verifyApi from '../api/verify-api.jsx';
import signUpOrgDetailsApi from '../api/sign-up-org-details-api.jsx';
import signUpUserDetailsApi from '../api/sign-up-user-details-api.jsx';


import {jwtDecode}  from "jwt-decode";
import {ToastContext} from "./ToastContext.jsx";
import {JWT, ROLES} from "../constants/constants.jsx";

// Always handle token expiry: either catch 401 responses in an axios response interceptor and attempt refresh or redirect to login.
export const AuthContext   = createContext(null);


export default function AuthProvider({ children }){
    const [user, setUser] = useState(null);
    const [pendingUser, setPendingUser] = useState(null);
    const [loading, setLoading] = useState(true);

    const {showToast} = useContext(ToastContext)

    const signIn = async (email, password, role) => {
        try {
            const res = await signInApi({email, password, role});

            if (!res?.success) {
                return { success: false, message: res?.message || "Sign in failed" };
            }

            setUser(res.user)
            return {success: true, data: res.user}
        }
        catch(err){
            return {success: false, message: err.response?.data?.error};
        }
    }

    const signUp = async (email, password, role, details) => {

        try {
            const res = await signUpApi({email, password, role});

            if (!res?.success) {
                return { success: false, message: res?.message || "Sign up failed" };
            }

            sessionStorage.setItem('pendingUser', JSON.stringify({ email: email, role:role, details:details }));
            setPendingUser({email, role, details})
            return {success: true}
        }
        catch (err){
            console.log(err);
            return {success: false, message: err.response?.data?.error};
        }
    }


    const signOut = async () => {
        signOutApi();
        setUser(null);
        setPendingUser(null);
        sessionStorage.removeItem(JWT.STORAGE_NAME);
    }

    const verifyEmail = async ( email, code ) => {

        try {
            const codeInt = Number(code.join(""));
            const res = await verifyApi({ email, code:codeInt });

            if (!res?.success) {
                return { success: false, message: res?.message || "Sign up failed" };
            }

            setUser(res.user)
            //We're logged in, now send the rest of sign up info
            if (res.user.role === ROLES.USER) {
                const details = pendingUser.details;

                const resDetails = await signUpUserDetailsApi(details);
                if (resDetails.success) {
                    showToast("Success", "User details saved successfully!", "success")
                }
            }
            else if (res.user.role === ROLES.ORGANIZATION) {
                const details = pendingUser.details;
                const resDetails = await signUpOrgDetailsApi(details);
                if (resDetails.success) {
                    showToast("Success", "Organization details saved successfully!", "success")
                }
            }

            setPendingUser(null);
            sessionStorage.removeItem("pendingUser");

            return { success: true };
        } catch (err) {
            return { success: false, message: err.response?.data?.error };
        }
    };

    const [pendingRestored, setPendingRestored] = useState(false);

    useEffect(() => {
        const savedPending = sessionStorage.getItem('pendingUser');


        if (savedPending) {
            const { email, role, details } = JSON.parse(savedPending);
            setPendingUser({ email, role, details });
        }
        setPendingRestored(true);
    }, []);


    useEffect(()=>{
        const token = sessionStorage.getItem(JWT.STORAGE_NAME);
        // console.log(token);
        // const token = null; // uncomment this if you want to sign out

        if (!token){
            setLoading(false);
            return;
        }

        try{
            const userData = jwtDecode(token); // returns { id, email, role, iat }
            if (userData.exp * 1000 < Date.now()) {
                // token expired
                sessionStorage.removeItem(JWT.STORAGE_NAME);
                setUser(null);
            }
            else {
                setUser({
                    id: userData.id,
                    email: userData.email,
                    role: userData.role.replace("ROLE_", ""),
                    profileComplete: userData.profileComplete
                });
            }

        }
        catch(err){
            console.log(err);
            sessionStorage.removeItem(JWT.STORAGE_NAME); // invalid token
        }
        finally {
            setLoading(false);
        }
    }, [])

    return(
        <AuthContext.Provider value={{ user, setUser, pendingUser, pendingRestored, loading, signIn, signUp, signOut, verifyEmail, isAuthenticated: !!user}}>
            {children}
        </AuthContext.Provider>
    )

}