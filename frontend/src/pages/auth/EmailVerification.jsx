import './style/EmailVerification.css';
import './style/AuthForm.css';

import { useState, useRef, useEffect, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { MailCheck } from 'lucide-react';

import { AuthContext } from '../../context/AuthContext.jsx';
import { ToastContext } from '../../context/ToastContext.jsx';
import { PATHS, ROLES } from '../../constants/constants.jsx';
import AuthLayout from './AuthLayout.jsx';

const regexDigit = /^[0-9]$/;

const EmailVerification = () => {
    const navigate = useNavigate();
    const [code, setCode] = useState(Array(6).fill(''));
    const [verifying, setVerifying] = useState(false);
    // codeInputRef is an array of references, each element is a reference to a code input box
    const codeInputRef = useRef(Array(6));

    const { pendingUser, verifyEmail, user } = useContext(AuthContext);
    const { showToast } = useContext(ToastContext);
    const email = pendingUser?.email;

    const handleSubmitCode = async (fullCode) => {
        setVerifying(true);
        const res = await verifyEmail(email, fullCode);
        setVerifying(false);

        if (res.success) {
            if (user.role === ROLES.USER) navigate(PATHS.HOME);
            else if (user.role === ROLES.ORGANIZATION) navigate(PATHS.ORGANIZATION.SUBMIT_REQUEST);
        } else {
            showToast('Verification failed.', res.message || 'Verification failed. Please try again.', 'error');
        }
    };

    // Checks each time the code changes, so it sends the code to backend once all boxes are filled
    useEffect(() => {
        if (!pendingUser) return;
        const allBoxesFilled = code.every((digit) => regexDigit.test(digit));
        if (allBoxesFilled) handleSubmitCode(code);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [code, pendingUser]);

    if (!pendingUser) return null; // render nothing while redirecting

    // When a user inputs a digit into a box, focus moves to the next box
    const handleChange = (e, i) => {
        const inputValue = e.target.value;
        if (regexDigit.test(inputValue)) {
            const newCode = [...code];
            newCode[i] = inputValue;
            setCode(newCode);
            if (i < 5) codeInputRef.current[i + 1].focus();
        }
    };

    // When a user presses backspace, focus moves to the previous box
    const handleBackspace = (e, i) => {
        if (e.key === 'Backspace' && i >= 0) {
            const newCode = [...code];
            newCode[i] = '';
            setCode(newCode);
            if (i > 0) codeInputRef.current[i - 1].focus();
        }
    };

    return (
        <AuthLayout cardWidth={480}>
            <div className="auth-form email-verification">
                <div className="email-verification__icon"><MailCheck size={28} /></div>
                <h1>Check your email</h1>
                <p className="auth-form__subtitle">
                    We sent a 6-digit code to <b>{email}</b>. Enter it below to verify your account.
                </p>

                <div className="EmailVerification-code-container" aria-busy={verifying}>
                    {code.map((item, i) => (
                        <input
                            type="text"
                            inputMode="numeric"
                            key={i}
                            value={item}
                            pattern="[0-9]"
                            maxLength="1"
                            ref={(el) => (codeInputRef.current[i] = el)}
                            disabled={verifying}
                            onChange={(e) => handleChange(e, i)}
                            onKeyDown={(e) => handleBackspace(e, i)}
                        />
                    ))}
                </div>

                <p className="auth-form__subtitle">Can't find the email? Check your <b>spam folder</b>.</p>
            </div>
        </AuthLayout>
    );
};

export default EmailVerification;
