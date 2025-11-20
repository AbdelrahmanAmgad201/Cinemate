import {useState, useEffect, createContext} from 'react'
import signInApi from '../api/signInApi.jsx';
import signOutApi from '../api/signOutApi.jsx';

import {jwtDecode}  from "jwt-decode";

// Always handle token expiry: either catch 401 responses in an axios response interceptor and attempt refresh or redirect to login.
export const AuthContext   = createContext(null);


export default function AuthProvider({ children }){
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    const signIn = async (email, password, role) => {
        try {
            const res = await signInApi({email, password, role});
            console.log(res);
            setUser(res.user)
            return {success: true}
        }
        catch(err){
            console.log(err);
            return {success: false, message: err.response?.data?.error};
        }
    }

    const signOut = async () => {
        signOutApi();
        setUser(null);
    }

    useEffect(()=>{
        const token = localStorage.getItem('token');

        if (!token){
            setLoading(false);
            return;
        }

        try{
            const userData = jwtDecode(token); // returns { id, email, role, iat }
            console.log(userData + " ,Upon fresh reload");
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
        <AuthContext.Provider value={{ user, loading, signIn, signOut, isAuthenticated: !!user}}>
            {children}
        </AuthContext.Provider>
    )

}