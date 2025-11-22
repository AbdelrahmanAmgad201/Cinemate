import './style/signUp.css';
import './style/signIn.css';
import React from 'react';
import { useNavigate } from 'react-router-dom';
import {useState, useContext} from 'react';
import { FcGoogle } from "react-icons/fc";
import oauthSignIn from '../../api/oauthSignInApi.jsx';
import { Link } from "react-router-dom";
import { AuthContext } from "../../context/AuthContext.jsx";
import { LuEyeOff, LuEye } from "react-icons/lu";


export default function SignIn({role = "User",
                                button1 = "Organization",
                                navigate1 = "/org-sign-in",
                                button2 = "Admin",
                                navigate2 = "/admin-sign-in",
                                showParagraph = true,
                                link = "/user-sign-up"}) {
    const navigate = useNavigate();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);

    const { user, loading, signIn, signOut, isAuthenticated } = useContext(AuthContext);

    const handleSubmit = async (e) => {
        e.preventDefault();
        const signInResult = await signIn(email, password, role.toUpperCase())
        // DELETE THIS LINE AFTER TESTING
        console.log("Sign In API " + {email:email, password:password, role:role.toUpperCase()});

        if (signInResult.success){
            navigate("/home-page")
        }
        else{
            alert("Error at sign in: see console ");
            console.log("Error at sign up: " + signInResult.message);
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
                    <input type = "text" id = "email" name = "email" placeholder="Enter your email address" required onChange = {(e) => {setEmail(e.target.value)}} />
                </div>
                <div className = "input-elem">
                    <label htmlFor = "password">Password</label>
                    <div className = "passwordWrapper">
                        <input type = {showPassword ? "text" : "password"} id = "password" name = "password" placeholder="Enter your password" required onChange = {(e) => {setPassword(e.target.value)}}/>
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

