import './style/signUp.css';
import { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { Link } from "react-router-dom";
import { AuthContext } from "../../context/AuthContext.jsx";

// Icons
import { FcGoogle } from "react-icons/fc";
import { HiOutlineMail, HiOutlineLockClosed } from "react-icons/hi";
import { AiOutlineUser } from "react-icons/ai";
import { CiCalendar } from "react-icons/ci";
import { LuEyeOff, LuEye } from "react-icons/lu";

export default function UserSignUp ({role = "User", show = true, link = "/"}) {

    const navigate = useNavigate();

    const [orgName, setOrgName] = useState("");
    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const [birthDate, setBirthDate] = useState("");
    const [gender, setGender] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [about, setAbout] = useState("");
    const [errors, setErrors] = useState({});
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);

    const { user, loading, signIn, signOut, signUp, isAuthenticated } = useContext(AuthContext);

    function buildRequestBody(role) {
        if (role === "USER") {
            return {
                firstName,
                lastName,
                birthday: birthDate,
                gender: gender.toUpperCase(),
                about,
            };
        }

        if (role === "ORGANIZATION") {
            return {
                name: orgName,
                about,
            };
        }
    }

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (password !== confirmPassword) {
            setErrors({password: "Passwords do not match"}); 
            setPassword("");
            setConfirmPassword("");   
            console.log("Passwords do not match");
            return;
        }

        setErrors({});

        const signUpResult = await signUp(email, password, role.toUpperCase(), buildRequestBody(role.toUpperCase()));
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
                            <input type="radio" id="male" name="gender" value="MALE" required onChange={(e) => {setGender(e.target.value)}} />
                            <label htmlFor="male">Male</label>
                            <input type="radio" id="female" name="gender" value="FEMALE" required onChange={(e) => {setGender(e.target.value)}} />
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

                <div className="input-elem">
                    <label htmlFor="password">Password</label>
                    <div className="icon-input">
                        <HiOutlineLockClosed />
                        <input type={showPassword ? "text" : "password"} id="password" name="password" placeholder="Enter your Password" required onChange={(e) => {setPassword(e.target.value)}} value={password} />
                        <span className="password-toggle-icon" onClick={() => setShowPassword(!showPassword)} style={{cursor: "pointer"}}>
                            {showPassword ? <LuEye /> : <LuEyeOff />}
                        </span>
                    </div>
                </div>
                {errors.password && <span className="error-message" style={{color: "#ff6b6b", marginTop: "0"}}>{errors.password}</span>}

                <div className="input-elem">
                    <label htmlFor="confirmPassword">Confirm Password</label>
                    <div className="icon-input">
                        <HiOutlineLockClosed />
                        <input type={showConfirmPassword ? "text" : "password"} id="confirmPassword" name="confirmPassword" placeholder="Confirm your Password" required onChange={(e) => {setConfirmPassword(e.target.value)}} value={confirmPassword} />
                        <span className="password-toggle-icon" onClick={() => setShowConfirmPassword(!showConfirmPassword)} style={{cursor: "pointer"}}>
                            {showConfirmPassword ? <LuEye /> : <LuEyeOff />}
                        </span>
                    </div>
                   
                </div>
                {errors.password && <span className="error-message" style={{color: "#ff6b6b", marginTop: "0"}}>{errors.password}</span>}
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