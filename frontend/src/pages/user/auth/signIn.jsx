import './style/signUp.css';
import React from 'react';
import { useNavigate } from 'react-router-dom';
import {useState} from 'react';
import { FcGoogle } from "react-icons/fc";

const UserSignIn = () => {
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
                    User Sign In
                </h1>
                <p>If you don't have an account register <br /> you can <a href = "/userSignUp">Register here!</a> </p>
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
                    <button type="submit" onClick={() => navigate("/orgSignIn")}>Sign in as Organization</button>
                    <button type="submit" onClick={() => navigate("/adminSignIn")}>Sign in as Admin</button>
                </div>
            </form>
            <button type="button" onClick={() => alert("Google")}><FcGoogle />Sign in using Google</button>

        </div>
    );
};

export default UserSignIn;