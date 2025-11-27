import {useState, useRef, useEffect, useContext} from 'react';
import { useNavigate } from 'react-router-dom';
import './style/emailVerification.css';
import {AuthContext} from "../../context/authContext.jsx";
import verifyApi from "../../api/verifyApi.jsx";
import {ErrorToastContext} from "../../context/errorToastContext.jsx";

const regexDigit =  /^[0-9]$/

const EmailVerification = () => {
    const navigate = useNavigate();
    const [code, setCode] = useState(Array(6).fill(''));
    // codeInputRef is an array of references, each element is s reference to a code input box
    const codeInputRef = useRef(Array(6));
    const [buttonTimer, setButtonTimer] = useState(0);

    const { pendingUser, verifyEmail, user } = useContext(AuthContext);
    const { showError } = useContext(ErrorToastContext);

    if (!pendingUser) return null; // render nothing while redirecting
    const email = pendingUser.email;

    // Checks each time the code changes, so it sends the code to backend if all boxed are filled
    useEffect(() => {
        const allBoxedFilled = code.every(digit => regexDigit.test(digit));
        if (allBoxedFilled) {
            handleSubmitCode()
        }
    }, [code]);

    const handleSubmitCode = async () => {

        const res = await verifyEmail(email, code);
        if (res.success === true) {
            if (user.role === "USER") navigate("/home-page")
            else if (user.role === "ORGANIZATION") navigate("/org-add-movie")
        }
        else {
            showError("Verification failed.", res.message || "Verification failed. Please try again.")
        }
    }

    // When a user inputs a digit into a box, the focus is switched to the next box
    const handleChange = (e, i) => {
        const inputValue = e.target.value;

        // test if the input is between 0 and 9
        if (regexDigit.test(inputValue)) {
            const newCode = [...code]
            newCode[i] = inputValue
            setCode(newCode)

            if (i < 5){
                // change focus to next box
                codeInputRef.current[i+1].focus();
            }
        }
    }


    // When a user presses backspace, the focus is shifted to the previous box
    const handleBackspace = (e, i) => {
        if (e.key === "Backspace" && i >= 0) {
            const newCode = [...code]
            newCode[i] = ''
            setCode(newCode)
            if (i>0)
                codeInputRef.current[i-1].focus();
        }
    }

    // Handles button timer
    useEffect(() => {
        if (buttonTimer === 0){
            return;
        }

        const intervalID = setInterval(
            () => {
                setButtonTimer(buttonTimer-1);
            }, 1000
        )

        // Runs when the useEffect finishes
        return () => clearInterval(intervalID);

    }, [buttonTimer]);

    const handleResendClick = () => {
        alert("Hello There")
        setButtonTimer(60);

        // TODO: HANDLE BACK END INTEGRATION

    }

    return (

        <div className="EmailVerification-container">

            <h1>Please verify your email</h1>

            <p>We sent an email to <b>{email}</b> containing a verification code</p>


            <div className="EmailVerification-code-container">
                {code.map((item, i) => (
                    <input
                        type="text"
                        key={i}
                        value={item}
                        pattern="[0-9]"
                        maxLength="1"
                        ref={e => ( codeInputRef.current[i] = e)}

                        onChange={e => handleChange(e, i)}
                        onKeyDown={e => handleBackspace(e, i)}
                    />
                ))
                }
            </div>


            <p>Can't find the email? Check your <b>spam folder</b></p>

            <p>Still can't find the email?</p>

            <button className="Resend-Verification-Email-button"
                    onClick={handleResendClick}
                    disabled={buttonTimer > 0}>
                {buttonTimer > 0 ?
                    `Wait ${buttonTimer}s` : "Resend Verification Email"
                }
            </button>

            {/*<button type="submit" onClick={() => navigate("/homePage")}>Done</button>*/}
        </div>
    );
};

export default EmailVerification;