import {useState, useEffect, createContext} from 'react'
import signInApi from '../api/signInApi.jsx';
import signOutApi from '../api/signOutApi.jsx';
import signUpApi from '../api/signUpApi.jsx';
import verifyApi from '../api/verifyApi.jsx';

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

            setUser(res.user)
            return {success: true}
        }
        catch(err){
            console.log(err);
            return {success: false, message: err.response?.data?.error};
        }
    }

    const signUp = async (email, password, role) => {

        try {
            const res = await signUpApi({email, password, role});

            setPendingUser(res.user)
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

    const verifyEmail = async ({ email, code }) => {
        // TODO: Might need to change code datatype
        try {
            const res = await verifyApi({ email, code });

            // verification success → backend returns ...

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
            setPendingUser(JSON.parse(savedPending));
        }
        setPendingRestored(true);
    }, []);


    useEffect(()=>{
        const token = localStorage.getItem('token');

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
                    role: userData.role,
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
        <AuthContext.Provider value={{ user, pendingUser, pendingRestored, loading, signIn, signUp, signOut, verifyEmail, isAuthenticated: !!user}}>
            {children}
        </AuthContext.Provider>
    )

}