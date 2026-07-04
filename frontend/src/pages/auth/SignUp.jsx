import './style/AuthForm.css';
import { useState, useContext } from 'react';
import { useNavigate, Link } from 'react-router-dom';

import { FcGoogle } from 'react-icons/fc';
import { Eye, EyeOff, Mail, Lock, Calendar, Building2, User } from 'lucide-react';

import oauthSignIn from '../../api/oauth-sign-in-api.js';
import { AuthContext } from '../../context/AuthContext.jsx';
import { ToastContext } from '../../context/ToastContext.jsx';
import { MAX_LENGTHS, MAX_VALUES, MIN_LENGTHS, PATHS, ROLES } from '../../constants/constants.jsx';
import AuthLayout from './AuthLayout.jsx';
import Input from '../../components/ui/Input.jsx';
import Textarea from '../../components/ui/Textarea.jsx';
import Button from '../../components/ui/Button.jsx';

const MIN_BIRTHDATE = new Date(new Date().setFullYear(new Date().getFullYear() - MAX_VALUES.BIRTHYEARS)).toISOString().split('T')[0];
const MAX_BIRTHDATE = new Date().toISOString().split('T')[0];

export default function UserSignUp({ role = 'User', show = true, link = PATHS.ROOT }) {
    const navigate = useNavigate();

    const [orgName, setOrgName] = useState('');
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [birthDate, setBirthDate] = useState('');
    const [gender, setGender] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [about, setAbout] = useState('');
    const [errors, setErrors] = useState({});
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [submitting, setSubmitting] = useState(false);

    const { signUp } = useContext(AuthContext);
    const { showToast } = useContext(ToastContext);

    function buildRequestBody(currentRole) {
        if (currentRole === ROLES.USER) {
            return { firstName, lastName, birthday: birthDate, gender: gender.toUpperCase(), about };
        }
        if (currentRole === ROLES.ORGANIZATION) {
            return { name: orgName, about };
        }
        return undefined;
    }

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (password !== confirmPassword) {
            setErrors({ password: 'Passwords do not match' });
            setPassword('');
            setConfirmPassword('');
            return;
        }

        setErrors({});
        setSubmitting(true);
        const signUpResult = await signUp(email, password, role.toUpperCase(), buildRequestBody(role.toUpperCase()));
        setSubmitting(false);

        if (signUpResult.success) {
            navigate(PATHS.EMAIL_VERIFICATION);
        } else {
            showToast('Sign up failed.', signUpResult.message || 'Sign up failed. Please try again.', 'error');
        }
    };

    return (
        <AuthLayout>
            <form className="auth-form" onSubmit={handleSubmit}>
                <h1>{role} sign up</h1>
                <p className="auth-form__subtitle">
                    Already have an account? <Link to={link}>Sign in</Link>
                </p>

                {!show && (
                    <Input
                        label="Organization name"
                        maxLength={MAX_LENGTHS.INPUT}
                        placeholder="Enter your organization name"
                        required
                        icon={<Building2 size={16} />}
                        onChange={(e) => setOrgName(e.target.value)}
                    />
                )}

                {show && (
                    <div className="auth-form__row">
                        <Input
                            label="First name"
                            maxLength={MAX_LENGTHS.INPUT}
                            required
                            icon={<User size={16} />}
                            onChange={(e) => setFirstName(e.target.value)}
                        />
                        <Input
                            label="Last name"
                            maxLength={MAX_LENGTHS.INPUT}
                            required
                            icon={<User size={16} />}
                            onChange={(e) => setLastName(e.target.value)}
                        />
                    </div>
                )}

                {show && (
                    <div className="auth-form__row">
                        <Input
                            label="Date of birth"
                            type="date"
                            required
                            icon={<Calendar size={16} />}
                            min={MIN_BIRTHDATE}
                            max={MAX_BIRTHDATE}
                            onChange={(e) => setBirthDate(e.target.value)}
                        />
                        <div className="auth-form__gender">
                            <span className="field__label">Gender</span>
                            <div className="auth-form__gender-options">
                                {['MALE', 'FEMALE'].map((option) => (
                                    <button
                                        type="button"
                                        key={option}
                                        className={`auth-form__gender-option ${gender === option ? 'auth-form__gender-option--selected' : ''}`}
                                        onClick={() => setGender(option)}
                                        aria-pressed={gender === option}
                                    >
                                        {option === 'MALE' ? 'Male' : 'Female'}
                                    </button>
                                ))}
                            </div>
                        </div>
                    </div>
                )}

                <Input
                    label="Email"
                    type="email"
                    maxLength={MAX_LENGTHS.INPUT}
                    placeholder="Enter your email address"
                    required
                    icon={<Mail size={16} />}
                    onChange={(e) => setEmail(e.target.value)}
                />

                <Input
                    label="Password"
                    type={showPassword ? 'text' : 'password'}
                    minLength={MIN_LENGTHS.PASSWORD}
                    maxLength={MAX_LENGTHS.INPUT}
                    placeholder="Enter your password"
                    required
                    value={password}
                    icon={<Lock size={16} />}
                    onChange={(e) => setPassword(e.target.value)}
                    rightIcon={showPassword ? <Eye size={16} /> : <EyeOff size={16} />}
                    onRightIconClick={() => setShowPassword((s) => !s)}
                    rightIconLabel={showPassword ? 'Hide password' : 'Show password'}
                    error={errors.password}
                />

                <Input
                    label="Confirm password"
                    type={showConfirmPassword ? 'text' : 'password'}
                    minLength={MIN_LENGTHS.PASSWORD}
                    maxLength={MAX_LENGTHS.INPUT}
                    placeholder="Confirm your password"
                    required
                    value={confirmPassword}
                    icon={<Lock size={16} />}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    rightIcon={showConfirmPassword ? <Eye size={16} /> : <EyeOff size={16} />}
                    onRightIconClick={() => setShowConfirmPassword((s) => !s)}
                    rightIconLabel={showConfirmPassword ? 'Hide password' : 'Show password'}
                />

                {!show && (
                    <Textarea
                        label="About"
                        required
                        placeholder="About your organization"
                        value={about}
                        onChange={(e) => setAbout(e.target.value)}
                    />
                )}

                <Button type="submit" fullWidth size="lg" loading={submitting}>Create account</Button>

                <div className="auth-form__divider"><span>or</span></div>
                <Button variant="secondary" fullWidth size="lg" icon={<FcGoogle size={18} />} onClick={oauthSignIn}>
                    Sign up with Google
                </Button>
            </form>
        </AuthLayout>
    );
}
