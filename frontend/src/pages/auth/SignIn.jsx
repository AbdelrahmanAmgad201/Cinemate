import './style/AuthForm.css';

import { Link, useNavigate } from 'react-router-dom';
import { useState, useContext } from 'react';
import { FcGoogle } from 'react-icons/fc';
import { Eye, EyeOff, Mail, Lock } from 'lucide-react';

import oauthSignIn from '../../api/oauth-sign-in-api.js';
import { AuthContext } from '../../context/AuthContext.jsx';
import { ToastContext } from '../../context/ToastContext.jsx';
import { MAX_LENGTHS, PATHS, ROLES } from '../../constants/constants.jsx';
import AuthLayout from './AuthLayout.jsx';
import Input from '../../components/ui/Input.jsx';
import Button from '../../components/ui/Button.jsx';

export default function SignIn({
    role = 'User',
    button1 = 'Organization',
    navigate1 = PATHS.ORGANIZATION.SIGN_IN,
    button2 = 'Admin',
    navigate2 = PATHS.ADMIN.SIGN_IN,
    showParagraph = true,
    link = PATHS.USER.SIGN_UP,
}) {
    const navigate = useNavigate();

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [submitting, setSubmitting] = useState(false);

    const { signIn } = useContext(AuthContext);
    const { showToast } = useContext(ToastContext);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSubmitting(true);
        const signInResult = await signIn(email, password, role.toUpperCase());
        setSubmitting(false);

        if (signInResult.success) {
            if (signInResult.data.role === ROLES.USER) navigate(PATHS.HOME);
            else if (signInResult.data.role === ROLES.ORGANIZATION) navigate(PATHS.ORGANIZATION.SUBMIT_REQUEST);
            else if (signInResult.data.role === ROLES.ADMIN) navigate(PATHS.ADMIN.REVIEW_REQUESTS);
        } else {
            showToast('Sign in failed.', signInResult.message || 'Sign in failed. Please try again.', 'error');
        }
    };

    return (
        <AuthLayout>
            <form className="auth-form" onSubmit={handleSubmit}>
                <h1>{role} sign in</h1>
                {showParagraph && (
                    <p className="auth-form__subtitle">
                        Don't have an account? <Link to={link}>Register here</Link>
                    </p>
                )}

                <Input
                    label="Email"
                    type="text"
                    maxLength={MAX_LENGTHS.INPUT}
                    placeholder="Enter your email address"
                    required
                    icon={<Mail size={16} />}
                    onChange={(e) => setEmail(e.target.value)}
                />

                <Input
                    label="Password"
                    type={showPassword ? 'text' : 'password'}
                    maxLength={MAX_LENGTHS.INPUT}
                    placeholder="Enter your password"
                    required
                    icon={<Lock size={16} />}
                    onChange={(e) => setPassword(e.target.value)}
                    autoComplete="current-password"
                    rightIcon={showPassword ? <Eye size={16} /> : <EyeOff size={16} />}
                    onRightIconClick={() => setShowPassword((s) => !s)}
                    rightIconLabel={showPassword ? 'Hide password' : 'Show password'}
                />

                <Button type="submit" fullWidth size="lg" loading={submitting}>Sign in</Button>

                <div className="auth-form__role-switch">
                    <button type="button" onClick={() => navigate(navigate1)}>Sign in as {button1}</button>
                    <button type="button" onClick={() => navigate(navigate2)}>Sign in as {button2}</button>
                </div>
            </form>

            {showParagraph && (
                <div className="auth-form__divider"><span>or</span></div>
            )}
            {showParagraph && (
                <Button variant="secondary" fullWidth size="lg" icon={<FcGoogle size={18} />} onClick={oauthSignIn}>
                    Sign in with Google
                </Button>
            )}
        </AuthLayout>
    );
}
