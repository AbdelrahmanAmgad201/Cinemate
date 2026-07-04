import { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { Calendar } from 'lucide-react';
import { AuthContext } from '../../context/AuthContext.jsx';
import { ToastContext } from '../../context/ToastContext.jsx';
import profileCompletionApi from '../../api/profile-completion-api.js';
import { PATHS, JWT, MAX_VALUES } from '../../constants/constants.jsx';
import { jwtDecode } from 'jwt-decode';
import AuthLayout from './AuthLayout.jsx';
import Input from '../../components/ui/Input.jsx';
import Button from '../../components/ui/Button.jsx';
import ProfileAvatar from '../../components/ProfileAvatar.jsx';
import './style/AuthForm.css';

const MIN_BIRTHDATE = new Date(new Date().setFullYear(new Date().getFullYear() - MAX_VALUES.BIRTHYEARS)).toISOString().split('T')[0];
const MAX_BIRTHDATE = new Date().toISOString().split('T')[0];

const ProfileCompletion = () => {
    const navigate = useNavigate();
    const { setUser } = useContext(AuthContext);
    const { showToast } = useContext(ToastContext);

    const [formData, setFormData] = useState({ birthday: '', gender: '' });
    const [loading, setLoading] = useState(false);

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!formData.birthday || !formData.gender) {
            return showToast('Error', 'Please fill in all required fields.', 'error');
        }

        setLoading(true);

        try {
            const response = await profileCompletionApi({
                birthday: formData.birthday,
                gender: formData.gender,
            });
            if (response.success) {
                const token = sessionStorage.getItem(JWT.STORAGE_NAME);
                const userData = jwtDecode(token);
                setUser({
                    id: userData.id,
                    email: userData.email,
                    role: userData.role.replace('ROLE_', ''),
                    profileComplete: true,
                });
                showToast('Success', 'Profile completed successfully!', 'success');
                navigate(PATHS.HOME);
            }
        } catch (error) {
            showToast('Error', error.message || 'Profile completion failed. Please try again.', 'error');
        } finally {
            setLoading(false);
        }
    };

    return (
        <AuthLayout>
            <ProfileAvatar />
            <form onSubmit={handleSubmit} className="auth-form">
                <h1>Complete your profile</h1>
                <p className="auth-form__subtitle">One last step before you can start browsing.</p>

                <Input
                    label="Date of birth"
                    type="date"
                    name="birthday"
                    required
                    icon={<Calendar size={16} />}
                    min={MIN_BIRTHDATE}
                    max={MAX_BIRTHDATE}
                    onChange={handleChange}
                />

                <div className="auth-form__gender">
                    <span className="field__label">Gender</span>
                    <div className="auth-form__gender-options">
                        {['MALE', 'FEMALE'].map((option) => (
                            <button
                                type="button"
                                key={option}
                                className={`auth-form__gender-option ${formData.gender === option ? 'auth-form__gender-option--selected' : ''}`}
                                onClick={() => setFormData({ ...formData, gender: option })}
                                aria-pressed={formData.gender === option}
                            >
                                {option === 'MALE' ? 'Male' : 'Female'}
                            </button>
                        ))}
                    </div>
                </div>

                <Button type="submit" fullWidth size="lg" loading={loading}>Continue</Button>
            </form>
        </AuthLayout>
    );
};

export default ProfileCompletion;
