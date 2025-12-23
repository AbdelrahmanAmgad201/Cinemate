import { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext.jsx';
import { ToastContext } from '../../context/ToastContext.jsx';
import profileCompletionApi from '../../api/profile-completion-api.jsx';
import { PATHS, JWT, MAX_VALUES } from '../../constants/constants.jsx';
import {jwtDecode}  from "jwt-decode";
import { CiCalendar } from "react-icons/ci";
import ProfileAvatar from '../../components/ProfileAvatar.jsx';
import "../auth/style/SignUp.css";

const ProfileCompletion = () => {
    const navigate = useNavigate();
    const { setUser } = useContext(AuthContext);
    const { showToast } = useContext(ToastContext);

    const [formData, setFormData] = useState({
        birthday: '',
        gender: '',
    });
    const [loading, setLoading] = useState(false);

    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value,
        });
    }

    const handleSubmit = async (e) => {
        e.preventDefault(); 

        if(!formData.birthday || !formData.gender){
            return showToast("Error", "Please fill in all required fields.", "error");
        }

        setLoading(true);

        try{
            const response = await profileCompletionApi({
                birthday: formData.birthday,
                gender: formData.gender,
            });
            if(response.success){
                const token = sessionStorage.getItem(JWT.STORAGE_NAME);
                const userData = jwtDecode(token);
                setUser({
                        id: userData.id,
                        email: userData.email,
                        role: userData.role.replace("ROLE_", ""),
                        profileComplete: true,
                    });
                showToast("Success", "Profile completed successfully!", "success");
                navigate(PATHS.HOME);
            }

        }
        catch(error){
            showToast("Error", error.message || "Profile completion failed. Please try again.", "error");
        }
        finally{
            setLoading(false);
        }
    }

    return(
        <div className="signup-container">
            <ProfileAvatar />   
            <form onSubmit={handleSubmit} className="profile-completion-form">
                <h1>Complete Your Profile</h1>
                <p>Please provide the following information to continue</p><br />
                <div className="date-gender">
                    <div className="input-elem">
                        <label htmlFor="birthDate">Date of Birth</label><br />
                        <div className="icon-input">
                            <CiCalendar />
                            <input type="date" id="birthDate" name="birthday" required style={{width: "200px"}}
                                onChange={handleChange}
                                min={new Date(new Date().setFullYear(new Date().getFullYear() - MAX_VALUES.BIRTHYEARS))
                                    .toISOString()
                                    .split("T")[0]}
                                max={new Date().toISOString().split("T")[0]}
                            />
                        </div>
                    </div>

                    <div className="input-elem" style={{borderBottom: "none"}}>
                        <label htmlFor="gender" style={{marginBottom: "23px"}}>Gender</label>
                        <div className="gender-options">
                            <input type="radio" id="male" name="gender" value="MALE" required onChange={handleChange} />
                            <label htmlFor="male">Male</label>
                            <input type="radio" id="female" name="gender" value="FEMALE" required onChange={handleChange} />
                            <label htmlFor="female">Female</label>
                        </div>
                    </div>
                </div>

                    <button type="submit">
                        {loading ? 'Saving...' : 'Continue'}
                    </button>
            </form>
        </div>
    )

};export default ProfileCompletion;