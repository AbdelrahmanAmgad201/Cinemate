import './style/signUp.css';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { AuthContext } from "../../../context/AuthContext";
import { useContext } from "react";

import signUpApi from '../../../api/signUpApi';

// Icons
import { FcGoogle } from "react-icons/fc";
import { HiOutlineMail } from "react-icons/hi";
import { AiOutlineUser } from "react-icons/ai";
import { HiOutlineLockClosed } from "react-icons/hi";
import { CiCalendar } from "react-icons/ci";

const UserSignUp = () => {

    const navigate = useNavigate();

    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const [birthDate, setBirthDate] = useState("");
    const [gender, setGender] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfrimPassword] = useState("");

    const { user, loading, signIn, signOut, isAuthenticated } = useContext(AuthContext);

    const handlesubmit = async (e) => {
        e.preventDefault();
        // The following is for testing and showing an example, THIS IS NOT FINALIZED
        const signUpResult = await signUpApi({email:email, password:password, role:"USER"});
        // await signIn({email:email, password:password, role:"USER"})
    }

    return (
        <div className="signup-container">
            <form onSubmit={handlesubmit}>
                <h1>
                    Sign Up
                </h1>
                <p>If you already have an account registered<br />You can <a href="/">Login here!</a></p>
                <div className="name">
                    <div className="input-elem">
                    <label htmlFor="firstName">First Name</label>
                    <input type="text" id="firstName" name="firstName" required style={{width: "200px"}} onChange={(e) => {setFirstName(e.target.value)}} />
                    </div>
                    
                    <div className="input-elem">
                    <label htmlFor="lastName">Last Name</label>
                    <input type="text" id="lastName" name="lastName" required style={{width: "200px"}} onChange={(e) => {setLastName(e.target.value)}} />
                    </div>
                </div>

                <div className="date-gender">
                    <div className="input-elem">
                        <label htmlFor="birthDate">Date of Birth</label><br />
                        <div className="icon-input">
                            <CiCalendar />
                            <input type="date" id="birthDate" name="birthDate" required style={{width: "200px"}} onChange={(e) => {setBirthDate(e.target.value)}} />
                        </div>
                    </div>

                    <div className="input-elem" style={{borderBottom: "none"}}>
                        <label htmlFor="gender" style={{marginBottom: "23px"}}>Gender</label>
                        <div className="gender-options">
                            <input type="radio" id="male" name="gender" required onChange={(e) => {setGender(e.target.value)}} />
                            <label htmlFor="male">Male</label>
                            <input type="radio" id="female" name="gender" required onChange={(e) => {setGender(e.target.value)}} />
                            <label htmlFor="female">Female</label>
                        </div>
                    </div>
                </div>

                <div className="input-elem">
                    <label htmlFor="email">Email</label>
                    <div className="icon-input">
                        <HiOutlineMail />
                        <input type="email" id="email" name="email" placeholder="Enter your email address" required onChange={(e) => {setEmail(e.target.value)}} />
                    </div>
                </div>

                <div className="input-elem">
                    <label htmlFor="username">Username</label>
                    <div className="icon-input">
                        <AiOutlineUser />
                        <input type="text" id="username" name="username" placeholder="Enter your username" required onChange={(e) => {setUsername(e.target.value)}} />
                    </div>
                </div>

                <div className="input-elem">
                    <label htmlFor="password">Password</label>
                    <div className="icon-input">
                        <HiOutlineLockClosed />
                        <input type="password" id="password" name="password" placeholder="Enter your Password" required onChange={(e) => {setPassword(e.target.value)}} />
                    </div>
                </div>

                <div className="input-elem">
                    <label htmlFor="confirmPassword">Confrim Password</label>
                    <div className="icon-input">
                        <HiOutlineLockClosed />
                        <input type="password" id="confirmPassword" name="confirmPassword" placeholder="Confirm your Password" required onChange={(e) => {setConfrimPassword(e.target.value)}} />
                    </div>
                </div>
                <button type="submit">Create Account</button><br />
                <button type="button" onClick={() => alert("Google")}><FcGoogle />Sign up using Google</button>
            </form>
        </div>
    );
};

export default UserSignUp;