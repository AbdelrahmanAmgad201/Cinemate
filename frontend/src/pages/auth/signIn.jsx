import './style/signUp.css';
import './style/signIn.css';

import { Link, useNavigate } from "react-router-dom";
import {useState, useContext} from 'react';
import { FcGoogle } from "react-icons/fc";
import { LuEyeOff, LuEye } from "react-icons/lu";

import oauthSignIn from '../../api/oauth-sign-in-api.jsx';
import { AuthContext } from "../../context/authContext.jsx";
import {ToastContext} from "../../context/ToastContext.jsx";
import {MAX_LENGTHS, PATHS, ROLES} from "../../constants/constants.jsx";


export default function SignIn({role = "User",
                                button1 = "Organization",
                                navigate1 = PATHS.ORGANIZATION.SIGN_IN,
                                button2 = "Admin",
                                navigate2 = PATHS.ADMIN.SIGN_IN,
                                showParagraph = true,
                                link = PATHS.USER.SIGN_UP}) {
    const navigate = useNavigate();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);

    const { signIn } = useContext(AuthContext);
    const { showToast } = useContext(ToastContext);

    const handleSubmit = async (e) => {
        e.preventDefault();
        const signInResult = await signIn(email, password, role.toUpperCase())
        console.log(signInResult);
        if (signInResult.success === true){
            if (signInResult.data.role === ROLES.USER) navigate(PATHS.HOME)
            else if (signInResult.data.role === ROLES.ORGANIZATION) navigate(PATHS.ORGANIZATION.SUBMIT_REQUEST)
            else if (signInResult.data.role === ROLES.ADMIN) navigate(PATHS.ADMIN.REVIEW_REQUESTS)
        }
        else{
            showToast("Sign in failed.", signInResult.message || "Sign in failed. Please try again.", "error")
        }
    }
    return (
        <div className = "signup-container signIn-container">
            <form onSubmit = {handleSubmit}>
                <h1>
                    {role} Sign In
                </h1>
                {showParagraph && <p>If you don't have an account register <br /> you can <Link to = {link}>Register here!</Link> </p>}
                <div className = "input-elem">
                    <label htmlFor = "email">Email</label>
                    <input type = "text" id = "email" name = "email" maxLength={MAX_LENGTHS.INPUT} placeholder="Enter your email address" required onChange = {(e) => {setEmail(e.target.value)}} />
                </div>
                <div className = "input-elem">
                    <label htmlFor = "password">Password</label>
                    <div className = "passwordWrapper">
                        <input type = {showPassword ? "text" : "password"} id = "password" name = "password" maxLength={MAX_LENGTHS.INPUT} placeholder="Enter your password" required onChange = {(e) => {setPassword(e.target.value)}}/>
                        <span className="password-toggle-icon" onClick={() => setShowPassword(!showPassword)} style={{cursor: "pointer"}}>
                            {showPassword ? <LuEye /> : <LuEyeOff />}
                        </span>
                    </div>
                </div>
                <button type="submit" >Sign In</button><br />
                <div className = "notUser">
                    <button type="submit" onClick={() => navigate(navigate1)}>Sign in as {button1}</button>
                    <button type="submit" onClick={() => navigate(navigate2)}>Sign in as {button2}</button>
                </div>
            </form>
            {showParagraph && 
            <div className="google-button" style={{width: "289px", margin: "30px auto"}}>
            <button type="button" onClick={oauthSignIn}><FcGoogle />Sign in using Google</button>
            </div>
            }
        </div>
    );
};

