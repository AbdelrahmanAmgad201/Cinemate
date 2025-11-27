import {useState, useEffect, createContext} from 'react'
import signInApi from '../api/signInApi.jsx';
import signOutApi from '../api/signOutApi.jsx';
import signUpApi from '../api/signUpApi.jsx';
import verifyApi from '../api/verifyApi.jsx';
import signUpOrgDetailsApi from '../api/signUpOrgDetailsApi.jsx';
import signUpUserDetailsApi from '../api/signUpUserDetailsApi.jsx';


import {jwtDecode}  from "jwt-decode";

// Always handle token expiry: either catch 401 responses in an axios response interceptor and attempt refresh or redirect to login.
export const AuthContext   = createContext(null);


export default function AuthProvider({ children }){
    const [user, setUser] = useState(null);
    const [pendingUser, setPendingUser] = useState(null);
    const [loading, setLoading] = useState(true);

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
            console.log(err);
            return {success: false, message: err.response?.data?.error};
        }
    }

    const signUp = async (email, password, role, details) => {

        try {
            const res = await signUpApi({email, password, role});

            if (!res?.success) {
                return { success: false, message: res?.message || "Sign up failed" };
            }

            localStorage.setItem('pendingUser', JSON.stringify({ email: email, role:role, details:details }));
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
        localStorage.removeItem('token');
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
            if (res.user.role === "USER") {
                const details = pendingUser.details;

                const resDetails = await signUpUserDetailsApi(details);
                if (resDetails.success) {
                    alert("User details saved successfully!");
                }
            }
            else if (res.user.role === "ORGANIZATION") {
                const details = pendingUser.details;
                const resDetails = await signUpOrgDetailsApi(details);
                if (resDetails.success) {
                    alert("Organization details saved successfully!");
                }
            }

            setPendingUser(null);
            localStorage.removeItem("pendingUser");

            return { success: true };
        } catch (err) {
            return { success: false, message: err.response?.data?.error };
        }
    };

    const [pendingRestored, setPendingRestored] = useState(false);

    useEffect(() => {
        const savedPending = localStorage.getItem('pendingUser');


        if (savedPending) {
            const { email, role, details } = JSON.parse(savedPending);
            setPendingUser({ email, role, details });
        }
        setPendingRestored(true);
    }, []);


    useEffect(()=>{
        const token = localStorage.getItem('token');
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
                localStorage.removeItem("token");
                setUser(null);
            }
            else {
                setUser({
                    id: userData.id,
                    email: userData.email,
                    role: userData.role.replace("ROLE_", ""),
                });
            }

        }
        catch(err){
            console.log(err);
            localStorage.removeItem("token"); // invalid token
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