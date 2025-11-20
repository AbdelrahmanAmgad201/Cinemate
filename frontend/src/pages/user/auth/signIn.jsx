import './style/signUp.css';
import React from 'react';
import { useNavigate } from 'react-router-dom';
import {useState} from 'react';
import { FcGoogle } from "react-icons/fc";

export default function SignIn({role = "User", button1 = "Organization", navigate1 = "/orgSignIn", button2 = "Admin", navigate2 = "/adminSignIn", showParagraph = true}) {
    const navigate = useNavigate();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);

    const handleSubmit = (e) => {
            e.preventDefault();
            console.log(email, password);
        }

    return (
        <div className = "signup-container">
            <form onSubmit = {handleSubmit}>
                <h1>
                    {role} Sign In
                </h1>
                {showParagraph && <p>If you don't have an account register <br /> you can <a href = "/userSignUp">Register here!</a> </p>}
                <div className = "input-elem">
                    <label htmlFor = "email">Email</label>
                    <input type = "text" id = "email" name = "email" placeholder="Enter your email address" required onChange = {(e) => {setEmail(e.target.value)}} />
                </div>
                <div className = "input-elem">
                    <label htmlFor = "password">Password</label>
                    <div className = "passwordWrapper">
                        <input type = {showPassword ? "text" : "password"} id = "password" name = "password" placeholder="Enter your password" required onChange = {(e) => {setPassword(e.target.value)}}/>
                        <button id = "showBtn" onClick = {() => setShowPassword(!showPassword)}>{showPassword ? "Hide" : "Show"}</button>
                    </div>
                </div>
                <button type="submit" onClick={() => navigate("/homePage")}>Sign In</button><br />
                <div className = "notUser">
                    <button type="submit" onClick={() => navigate(navigate1)}>Sign in as {button1}</button>
                    <button type="submit" onClick={() => navigate(navigate2)}>Sign in as {button2}</button>
                </div>
            </form>
            {showParagraph && <button type="button" onClick={() => alert("Google")}><FcGoogle />Sign in using Google</button>}

        </div>
    );
};

