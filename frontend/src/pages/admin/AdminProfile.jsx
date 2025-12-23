import React, {useContext, useEffect, useState} from "react";
import { AuthContext } from "../../context/AuthContext.jsx";
import NavBar from "../../components/OrgAdminNavBar.jsx";
import { LuEye, LuEyeOff, LuUser, LuMail, LuLock } from "react-icons/lu";
import "./style/AdminProfile.css";
import {ToastContext} from "../../context/ToastContext.jsx";
import {MAX_LENGTHS} from "../../constants/constants.jsx";
import {getAdminProfileApi, updateNameApi, updatePasswordApi} from "../../api/admin-api.jsx";

export default function AdminProfile() {
    const { user } = useContext(AuthContext);
    const [isEditable, setIsEditable] = useState(false);

    // Form States
    const [newName, setNewName] = useState(user?.name || "");
    const [name, setName] = useState(user?.name || "");
    const [newEmail, setNewEmail] = useState(user?.email || "");
    const [newPassword, setNewPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [oldPassword, setOldPassword] = useState("");
    const [showOldPassword, setShowOldPassword] = useState(false);

    const [showCodeInput, setShowCodeInput] = useState(false);
    const [verificationCode, setVerificationCode] = useState("");

    const {showToast} = useContext(ToastContext);

    const handleCancel = () => {
        setNewName(name);
        setNewEmail(user?.email || "");
        setIsEditable(false);
    };

    const handleRequestCode = async () => {
        setShowCodeInput(true);
    };

    const handleSave = async () => {
        const promises = [];
        let profileChanged = newName !== user.name;
        let passwordChanged = oldPassword && newPassword && oldPassword !== newPassword;

        if (profileChanged) {
            const profileData = { name: newName };
            promises.push(updateNameApi(profileData));
        }

        if (passwordChanged) {
            const passwordData = {
                oldPassword,
                newPassword,
            };
            promises.push(updatePasswordApi(passwordData));
        }

        if (promises.length === 0) {
            setIsEditable(false);
            return;
        }

            // Execute all necessary APIs at once
            const results = await Promise.all(promises);

            // Check if all succeeded
            const allSuccess = results.every(res => res.success);

            if (allSuccess) {
                showToast("Success", "Account updated successfully", "success");
                setIsEditable(false);
                setShowCodeInput(false);
                setName(newName);
            } else {
                showToast("Error", "One or more updates failed", "error");
            }
    };

    useEffect(() => {
        const fetchAdminData = async () => {
            const res = await getAdminProfileApi(); // Should return Admin

            if (res.success) {
                setName(res.data.name);
                // Also sync our "editing" states
                setNewName(res.data.name);
                setNewEmail(res.data.email);
            }
        };

        fetchAdminData();
    }, []);

    return (
        <div className="add-admin-page"> {/* Reusing the page container class */}
            <NavBar />

            <div className="profile-container">
                <div className="admin-profile-form">
                    <div className="profile-header">
                        <div className="profile-avatar-large">
                            {name.charAt(0) || "A"}
                        </div>
                        <h2>Admin Details</h2>
                    </div>

                    {/* Name Field */}
                    <div className="input-field">
                        <label><LuUser size={14} /> Full Name</label>
                        {isEditable ? (
                            <input
                                type="text"
                                value={newName}
                                onChange={(e) => setNewName(e.target.value)}
                                maxLength={MAX_LENGTHS.INPUT}
                            />
                        ) : (
                            <div className="view-mode-text">{name}</div>
                        )}
                    </div>

                    {/* Email Field */}
                    <div className="input-field">
                        <label><LuMail size={14} /> Email Address</label>
                        {/*{isEditable ? (*/}
                        {/*    <input*/}
                        {/*        type="email"*/}
                        {/*        value={newEmail}*/}
                        {/*        onChange={(e) => setNewEmail(e.target.value)}*/}
                        {/*        maxLength={MAX_LENGTHS.INPUT}*/}

                        {/*    />*/}
                        {/*) : (*/}
                            <div className="view-mode-text">{user?.email}</div>
                        {/*)}*/}
                    </div>

                    {/* Password Field */}
                    <div className="input-field password-section">
                        <label><LuLock size={14} /> Password</label>

                        {!isEditable ? (
                            <div className="view-mode-text password-masked">••••••••••••</div>
                        ) : (
                            <div className="password-edit-container">
                                {!showCodeInput ? (
                                    <button
                                        type="button"
                                        className="request-code-btn"
                                        onClick={handleRequestCode}
                                    >
                                        Change Password
                                    </button>
                                ) : (
                                    <div className="verification-flow">
                                        <div className="verification-flow">

                                            <div className="password-wrapper">
                                                <input
                                                    type={showOldPassword ? "text" : "password"}
                                                    placeholder="Enter current password"
                                                    onChange={(e) => setOldPassword(e.target.value)}
                                                    maxLength={MAX_LENGTHS.INPUT}
                                                />
                                                <span className="password-toggle-icon" onClick={() => setShowOldPassword(!showOldPassword)}>
                                                {showOldPassword ? <LuEye/> : <LuEyeOff/>}
                                            </span>
                                            </div>
                                        </div>
                                        <div className="password-wrapper">
                                            <input
                                                type={showPassword ? "text" : "password"}
                                                placeholder="Enter new password"
                                                onChange={(e) => setNewPassword(e.target.value)}
                                                maxLength={MAX_LENGTHS.INPUT}
                                                minLength={8}
                                            />
                                            <span className="password-toggle-icon" onClick={() => setShowPassword(!showPassword)}>
                                                {showPassword ? <LuEye/> : <LuEyeOff/>}
                                            </span>
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>

                    <div className="profile-actions">
                        {!isEditable ? (
                            <button className="primary-btn" onClick={() => setIsEditable(true)}>
                                Edit Profile
                            </button>
                        ) : (
                            <>
                                <button className="secondary-btn" onClick={handleCancel}>Cancel</button>
                                <button className="primary-btn" onClick={async () => {
                                    setIsEditable(false)
                                    await handleSave()
                                }}>Save Changes</button>
                            </>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}