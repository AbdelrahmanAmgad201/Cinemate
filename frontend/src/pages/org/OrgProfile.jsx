import { useContext, useEffect, useState } from 'react';
import NavBar from '../../components/OrgAdminNavBar';
import { ToastContext } from '../../context/ToastContext';
import { fetchOrgProfileApi, updateOrgProfileApi, updateOrgPasswordApi } from '../../api/org-analytics-api';
import './style/orgProfile.css';
import LoadingFallback from '../../components/LoadingFallback';
import { Building2, Eye, EyeOff, Lock, Pencil } from 'lucide-react';

import { MAX_LENGTHS, MIN_LENGTHS } from '../../constants/constants';
import Avatar from '../../components/ui/Avatar.jsx';
import Card from '../../components/ui/Card.jsx';
import Input from '../../components/ui/Input.jsx';
import Textarea from '../../components/ui/Textarea.jsx';
import Button from '../../components/ui/Button.jsx';

export default function OrgProfile() {
    const { showToast } = useContext(ToastContext);

    const [loading, setLoading] = useState(false);
    const [editMode, setEditMode] = useState(false);
    const [oldPassword, setOldPassword] = useState('');
    const [showOldPassword, setShowOldPassword] = useState(false);
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [confirmPassword, setConfirmPassword] = useState('');
    const [editData, setEditData] = useState({ name: '', about: '' });
    const [orgData, setOrgData] = useState({ name: '', about: '', email: '', createdAt: '' });

    const getOrgData = async () => {
        setLoading(true);
        const result = await fetchOrgProfileApi();
        if (result.success) {
            const data = result.response;
            setOrgData({ name: data.name, about: data.about, email: data.email, createdAt: data.createdAt });
        }
        setLoading(false);
    };

    useEffect(() => {
        getOrgData();
    }, []);

    const handleEdit = () => {
        setEditData(orgData);
        setEditMode(true);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (password || confirmPassword || oldPassword) {
            if (!oldPassword) return showToast('Failed to save', 'Please enter your old password.', 'error');
            if (password !== confirmPassword) return showToast('Failed to save', 'Passwords do not match.', 'error');
        }

        if (!editData.name) return showToast('Failed to save', "Name can't be empty", 'error');
        if (!editData.about) return showToast('Failed to save', "About section can't be empty", 'error');
        if (editData.about.length > MAX_LENGTHS.TEXTAREA) {
            return showToast('Failed to save', `About section cannot exceed ${MAX_LENGTHS.TEXTAREA} characters.`, 'error');
        }

        setLoading(true);
        const profileResult = await updateOrgProfileApi({ name: editData.name, about: editData.about });

        let passwordResult = { success: true };
        if (password && password.trim()) {
            passwordResult = await updateOrgPasswordApi(oldPassword, password);
        }

        if (profileResult.success && passwordResult.success) {
            showToast('Success', 'Profile updated successfully.', 'success');
            getOrgData();
        } else if (!passwordResult.success) {
            showToast('Warning', 'Profile updated successfully, without changing password.', 'warning');
            getOrgData();
        } else if (!profileResult.success && passwordResult.success) {
            showToast('Warning', `Password updated, but profile failed: ${profileResult.message}`, 'warning');
        } else {
            showToast('Error', 'Failed to update both profile and password.', 'error');
        }
        setLoading(false);
        setOrgData(editData);
        setEditMode(false);
    };

    if (loading) {
        return (
            <div>
                <NavBar />
                <div className="org-profile-container"><LoadingFallback fullScreen /></div>
            </div>
        );
    }

    return (
        <div>
            <NavBar />
            <div className="org-profile-container">
                {!editMode ? (
                    <div className="view-mode">
                        <Card padding="lg" className="org-summary-card">
                            <Avatar name={orgData.name} size="lg" />
                            <span className="org-summary-name">{orgData.name}</span>
                            <Button size="sm" variant="secondary" icon={<Pencil size={14} />} onClick={handleEdit}>Edit profile</Button>
                        </Card>
                        <Card padding="lg">
                            <h3 className="org-card-title">About</h3>
                            <p>{orgData.about}</p>
                        </Card>
                        <Card padding="lg">
                            <h3 className="org-card-title">Founded</h3>
                            <p>{orgData.createdAt?.substring(0, 4) || 'N/A'}</p>
                        </Card>
                    </div>
                ) : (
                    <form className="org-edit-form" onSubmit={handleSubmit}>
                        <Card padding="lg">
                            <h3 className="org-card-title">Edit profile</h3>
                            <Input
                                label="Name"
                                icon={<Building2 size={16} />}
                                value={editData.name}
                                minLength={MIN_LENGTHS.INPUT}
                                maxLength={MAX_LENGTHS.INPUT}
                                onChange={(e) => setEditData({ ...editData, name: e.target.value })}
                            />
                            <Textarea
                                label="About"
                                value={editData.about}
                                minLength={MIN_LENGTHS.INPUT}
                                maxLength={MAX_LENGTHS.TEXTAREA}
                                onChange={(e) => setEditData({ ...editData, about: e.target.value })}
                            />
                        </Card>
                        <Card padding="lg">
                            <h3 className="org-card-title">Change password</h3>
                            <Input
                                label="Old password"
                                type={showOldPassword ? 'text' : 'password'}
                                icon={<Lock size={16} />}
                                minLength={MIN_LENGTHS.PASSWORD}
                                maxLength={MAX_LENGTHS.INPUT}
                                placeholder="Enter your password"
                                onChange={(e) => setOldPassword(e.target.value)}
                                rightIcon={showOldPassword ? <Eye size={16} /> : <EyeOff size={16} />}
                                onRightIconClick={() => setShowOldPassword((s) => !s)}
                            />
                            <Input
                                label="New password"
                                type={showPassword ? 'text' : 'password'}
                                icon={<Lock size={16} />}
                                minLength={MIN_LENGTHS.PASSWORD}
                                maxLength={MAX_LENGTHS.INPUT}
                                placeholder="Enter new password"
                                onChange={(e) => setPassword(e.target.value)}
                                rightIcon={showPassword ? <Eye size={16} /> : <EyeOff size={16} />}
                                onRightIconClick={() => setShowPassword((s) => !s)}
                            />
                            <Input
                                label="Confirm new password"
                                type={showConfirmPassword ? 'text' : 'password'}
                                icon={<Lock size={16} />}
                                minLength={MIN_LENGTHS.PASSWORD}
                                maxLength={MAX_LENGTHS.INPUT}
                                placeholder="Confirm new password"
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                rightIcon={showConfirmPassword ? <Eye size={16} /> : <EyeOff size={16} />}
                                onRightIconClick={() => setShowConfirmPassword((s) => !s)}
                            />
                        </Card>

                        <div className="form-buttons">
                            <Button type="button" variant="secondary" onClick={() => setEditMode(false)}>Cancel</Button>
                            <Button type="submit">Save</Button>
                        </div>
                    </form>
                )}
            </div>
        </div>
    );
}
