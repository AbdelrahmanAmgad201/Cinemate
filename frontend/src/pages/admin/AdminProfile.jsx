import { useContext, useEffect, useState } from 'react';
import { AuthContext } from '../../context/AuthContext.jsx';
import NavBar from '../../components/OrgAdminNavBar.jsx';
import { Eye, EyeOff, User, Mail, Lock } from 'lucide-react';
import './style/AdminProfile.css';
import { ToastContext } from '../../context/ToastContext.jsx';
import { MAX_LENGTHS } from '../../constants/constants.jsx';
import { getAdminProfileApi, updateNameApi, updatePasswordApi } from '../../api/admin-api.js';
import Avatar from '../../components/ui/Avatar.jsx';
import Input from '../../components/ui/Input.jsx';
import Button from '../../components/ui/Button.jsx';

export default function AdminProfile() {
    const { user } = useContext(AuthContext);
    const [isEditable, setIsEditable] = useState(false);
    const [saving, setSaving] = useState(false);

    const [newName, setNewName] = useState(user?.name || '');
    const [name, setName] = useState(user?.name || '');
    const [newPassword, setNewPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [oldPassword, setOldPassword] = useState('');
    const [showOldPassword, setShowOldPassword] = useState(false);
    const [showPasswordFields, setShowPasswordFields] = useState(false);

    const { showToast } = useContext(ToastContext);

    const handleCancel = () => {
        setNewName(name);
        setIsEditable(false);
        setShowPasswordFields(false);
        setOldPassword('');
        setNewPassword('');
    };

    const handleSave = async () => {
        setSaving(true);
        const promises = [];
        const profileChanged = newName !== name;
        const passwordChanged = oldPassword && newPassword;

        if (profileChanged) promises.push(updateNameApi({ name: newName }));
        if (passwordChanged) promises.push(updatePasswordApi({ oldPassword, newPassword }));

        if (promises.length === 0) {
            setIsEditable(false);
            setSaving(false);
            return;
        }

        const results = await Promise.all(promises);
        const allSuccess = results.every((res) => res.success);

        if (allSuccess) {
            showToast('Success', 'Account updated successfully', 'success');
            setIsEditable(false);
            setShowPasswordFields(false);
            setName(newName);
        } else {
            showToast('Error', 'One or more updates failed', 'error');
        }
        setSaving(false);
    };

    useEffect(() => {
        getAdminProfileApi().then((res) => {
            if (res.success) {
                setName(res.data.name);
                setNewName(res.data.name);
            }
        });
    }, []);

    return (
        <div className="admin-profile-page">
            <NavBar />

            <div className="profile-container">
                <div className="admin-profile-form">
                    <div className="profile-header">
                        <Avatar name={name} size="xl" />
                        <h2>Admin details</h2>
                    </div>

                    {isEditable ? (
                        <Input label="Full name" icon={<User size={16} />} value={newName} onChange={(e) => setNewName(e.target.value)} maxLength={MAX_LENGTHS.INPUT} />
                    ) : (
                        <div className="input-field">
                            <label><User size={14} /> Full name</label>
                            <div className="view-mode-text">{name}</div>
                        </div>
                    )}

                    <div className="input-field">
                        <label><Mail size={14} /> Email address</label>
                        <div className="view-mode-text">{user?.email}</div>
                    </div>

                    <div className="input-field">
                        <label><Lock size={14} /> Password</label>
                        {!isEditable ? (
                            <div className="view-mode-text password-masked">••••••••••••</div>
                        ) : !showPasswordFields ? (
                            <button type="button" className="request-code-btn" onClick={() => setShowPasswordFields(true)}>
                                Change password
                            </button>
                        ) : (
                            <div className="password-edit-container">
                                <Input
                                    type={showOldPassword ? 'text' : 'password'}
                                    placeholder="Current password"
                                    onChange={(e) => setOldPassword(e.target.value)}
                                    maxLength={MAX_LENGTHS.INPUT}
                                    rightIcon={showOldPassword ? <Eye size={16} /> : <EyeOff size={16} />}
                                    onRightIconClick={() => setShowOldPassword((s) => !s)}
                                />
                                <Input
                                    type={showPassword ? 'text' : 'password'}
                                    placeholder="New password"
                                    onChange={(e) => setNewPassword(e.target.value)}
                                    maxLength={MAX_LENGTHS.INPUT}
                                    minLength={8}
                                    rightIcon={showPassword ? <Eye size={16} /> : <EyeOff size={16} />}
                                    onRightIconClick={() => setShowPassword((s) => !s)}
                                />
                            </div>
                        )}
                    </div>

                    <div className="profile-actions">
                        {!isEditable ? (
                            <Button onClick={() => setIsEditable(true)}>Edit profile</Button>
                        ) : (
                            <>
                                <Button variant="secondary" onClick={handleCancel}>Cancel</Button>
                                <Button onClick={handleSave} loading={saving}>Save changes</Button>
                            </>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
