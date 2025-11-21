import './style/signUp.css';
import { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { Link } from "react-router-dom";
import { AuthContext } from "../../context/AuthContext.jsx";

// Icons
import { FcGoogle } from "react-icons/fc";
import { HiOutlineMail } from "react-icons/hi";
import { AiOutlineUser } from "react-icons/ai";
import { HiOutlineLockClosed } from "react-icons/hi";
import { CiCalendar } from "react-icons/ci";
import signUpOrgDetails from "../../api/signUpOrgDetails.jsx";

export default function UserSignUp ({role = "User", show = true, link = "/"}) {

    const navigate = useNavigate();

    const [orgName, setOrgName] = useState("");
    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const [birthDate, setBirthDate] = useState("");
    const [gender, setGender] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfrimPassword] = useState("");
    const [about, setAbout] = useState("");

    const { user, loading, signIn, signOut, signUp, isAuthenticated } = useContext(AuthContext);

    const handleSubmit = async (e) => {
        e.preventDefault();

        const signUpResult = await signUp(email, password, role.toUpperCase());
        // TODO: Handle User Details API
        // signUpOrgDetails
        // signUpUserDetails

        // DELETE THIS LINE AFTER TESTING
        console.log("Sign Up API " + {email:email, password:password, role:role.toUpperCase()});

        if (signUpResult.success){
            navigate("/email-verification");
        }
        else{
            alert("Error at sign up: see console ");
            console.log("Error at sign up: " + signUpResult.message);
        }
    }

    return (
        <div className="signup-container">
            <form onSubmit={handleSubmit}>
                <h1>
                    {role} Sign Up
                </h1>
                <p>If you already have an account registered<br />You can <Link to={link}>Login here!</Link></p>
                {!show && <div className="input-elem">
                    <label htmlFor="orgName">Organization Name</label>
                    <input type="text" id="orgName" name="orgName" required placeholder = "Enter your organization name" onChange={(e) => {setOrgName(e.target.value)}} />
                </div>}
                {show && <div className="name">
                    <div className="input-elem">
                    <label htmlFor="firstName">First Name</label>
                    <input type="text" id="firstName" name="firstName" required style={{width: "200px"}} onChange={(e) => {setFirstName(e.target.value)}} />
                    </div>
                    
                    <div className="input-elem">
                    <label htmlFor="lastName">Last Name</label>
                    <input type="text" id="lastName" name="lastName" required style={{width: "200px"}} onChange={(e) => {setLastName(e.target.value)}} />
                    </div>
                </div>}

                {show && <div className="date-gender">
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
                </div>}

                <div className="input-elem">
                    <label htmlFor="email">Email</label>
                    <div className="icon-input">
                        <HiOutlineMail />
                        <input type="email" id="email" name="email" placeholder="Enter your email address" required onChange={(e) => {setEmail(e.target.value)}} />
                    </div>
                </div>

                {show && <div className="input-elem">
                    <label htmlFor="username">Username</label>
                    <div className="icon-input">
                        <AiOutlineUser />
                        <input type="text" id="username" name="username" placeholder="Enter your username" required onChange={(e) => {setUsername(e.target.value)}} />
                    </div>
                </div>}

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
                {!show && <div className="input-elem">
                    <label htmlFor="about">About</label>
                    <textarea id="about" name="about" required placeholder = "About your organization" onChange={(e) => {setAbout(e.target.value)}} />
                </div>}

                <button type="submit">Create Account</button><br />
                <button type="button" onClick={() => alert("Google")}><FcGoogle />Sign up using Google</button>
            </form>
        </div>
    );
};