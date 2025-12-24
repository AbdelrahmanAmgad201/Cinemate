import React, { useState, useContext } from 'react';
import { FaPencilAlt } from 'react-icons/fa';
import { updatePasswordApi } from '../api/admin-api.jsx';
import { ToastContext } from '../context/ToastContext.jsx';
import { MIN_LENGTHS } from '../constants/constants.jsx';

export default function PersonalData({ profile, user }) {
    const getVal = (key) => {
        if (key === 'email') return (profile && profile.email) || (user && user.email) || '';
        return (profile && profile[key]) || '';
    };

    const fields = [
        { k: 'firstName', label: 'First Name' },
        { k: 'lastName', label: 'Last Name' },
        { k: 'email', label: 'Email' },
        { k: 'aboutMe', label: 'About Me' },
    ];

    const { showToast } = useContext(ToastContext);
    const [editingPassword, setEditingPassword] = useState(false);
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [savingPassword, setSavingPassword] = useState(false);

    const handleEditClick = (key) => {
        if (key === 'password') setEditingPassword(true);
    };

    const handlePasswordSave = async () => {
        if (newPassword !== confirmPassword) {
            showToast('Password error', 'New passwords do not match', 'error');
            return;
        }
        if (newPassword.length < MIN_LENGTHS.PASSWORD) {
            showToast('Password error', `Password must be at least ${MIN_LENGTHS.PASSWORD} characters`, 'error');
            return;
        }
        setSavingPassword(true);
        try {
            const res = await updatePasswordApi({ oldPassword: currentPassword, newPassword });
            if (res?.success) {
                showToast('Password updated', 'Your password was updated successfully', 'success');
                setEditingPassword(false);
                setCurrentPassword('');
                setNewPassword('');
                setConfirmPassword('');
            } else {
                showToast('Update failed', res?.message || 'Failed to update password', 'error');
            }
        } catch (e) {
            console.error(e);
            showToast('Update failed', 'Unknown error', 'error');
        } finally {
            setSavingPassword(false);
        }
    };

    const handlePasswordCancel = () => {
        setEditingPassword(false);
        setCurrentPassword('');
        setNewPassword('');
        setConfirmPassword('');
    };

    return (
        <div className="personal-data">
            <div className="pd-grid">
                {fields.map(({ k, label }) => (
                    <div key={k} className={`pd-card ${k === 'aboutMe' ? 'pd-card--tall' : ''}`}>
                        {k !== 'email' && (
                            <button
                                type="button"
                                className="pd-edit-btn"
                                title={`Edit ${label}`}
                                aria-label={`Edit ${label}`}
                                onClick={() => handleEditClick(k)}
                            >
                                <FaPencilAlt />
                            </button>
                        )}

                        <div className="pd-label">{label}</div>
                        <div className="pd-value">
                            {k === 'aboutMe' ? (
                                <textarea
                                    className="pd-textarea"
                                    value={getVal(k)}
                                    readOnly
                                    rows={k === 'aboutMe' ? 6 : 3}
                                    aria-readonly="true"
                                />
                            ) : (
                                <input
                                    className="pd-input"
                                    type="text"
                                    value={getVal(k)}
                                    placeholder="—"
                                    readOnly
                                    aria-readonly="true"
                                />
                            )}
                        </div>
                    </div>
                ))}

                <div className="pd-card">
                    {!editingPassword && (
                        <>
                            <button
                                type="button"
                                className="pd-edit-btn"
                                title={`Change password`}
                                aria-label={`Change password`}
                                onClick={() => setEditingPassword(true)}
                            >
                                <FaPencilAlt />
                            </button>

                            <div className="pd-label">Password</div>
                            <div className="pd-value">
                                <input className="pd-input pd-password-display" type="password" value={"********"} readOnly aria-readonly="true" />
                            </div>
                        </>
                    )}

                    {editingPassword && (
                        <div>
                            <div className="pd-label">Change Password</div>
                            <div className="pd-value">
                                <div className="pd-password-inputs">
                                    <input
                                        className="pd-input"
                                        type="password"
                                        placeholder="Current password"
                                        value={currentPassword}
                                        onChange={(e) => setCurrentPassword(e.target.value)}
                                    />
                                    <input
                                        className="pd-input"
                                        type="password"
                                        placeholder="New password"
                                        value={newPassword}
                                        onChange={(e) => setNewPassword(e.target.value)}
                                    />
                                    <input
                                        className="pd-input"
                                        type="password"
                                        placeholder="Confirm new password"
                                        value={confirmPassword}
                                        onChange={(e) => setConfirmPassword(e.target.value)}
                                    />
                                </div>

                                <div className="pd-actions" style={{ marginTop: 8, display: 'flex', gap: 8 }}>
                                    <button className="btn btn-fill" type="button" onClick={handlePasswordSave} disabled={savingPassword}>{savingPassword ? 'Saving...' : 'Save'}</button>
                                    <button className="btn btn-small" type="button" onClick={handlePasswordCancel} disabled={savingPassword}>Cancel</button>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
