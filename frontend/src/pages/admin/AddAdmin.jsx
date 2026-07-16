import './style/AddAdmin.css';
import NavBar from '../../components/OrgAdminNavBar.jsx';

import { useContext, useState } from 'react';
import { ToastContext } from '../../context/ToastContext.jsx';
import { User, Mail, Lock, Eye, EyeOff } from 'lucide-react';

import { addAdminApi } from '../../api/admin-api.js';

import { MAX_LENGTHS } from '../../constants/constants.jsx';
import Card from '../../components/ui/Card.jsx';
import Input from '../../components/ui/Input.jsx';
import Button from '../../components/ui/Button.jsx';
import ConfirmDialog from '../../components/ui/ConfirmDialog.jsx';

export default function AddAdmin() {
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [submitting, setSubmitting] = useState(false);

    const { showToast } = useContext(ToastContext);

    const handleCreate = async () => {
        setSubmitting(true);
        const res = await addAdminApi({ name, email, password });
        setSubmitting(false);
        setConfirmOpen(false);

        if (res.success) {
            showToast('Sign up success.', 'New admin added successfully.', 'success');
            setName('');
            setEmail('');
            setPassword('');
        } else {
            showToast('Sign up failed.', res.message || 'Sign up failed. Please try again.', 'error');
        }
    };

    return (
        <div className="add-admin-page">
            <NavBar />

            <form onSubmit={(e) => { e.preventDefault(); setConfirmOpen(true); }} className="add-admin-form">
                <Card padding="lg">
                    <h1 className="add-admin-title">Add a new admin</h1>

                    <Input
                        label="Name"
                        icon={<User size={16} />}
                        value={name}
                        maxLength={MAX_LENGTHS.INPUT}
                        placeholder="Enter admin's name"
                        required
                        onChange={(e) => setName(e.target.value)}
                    />

                    <Input
                        label="Email"
                        type="email"
                        icon={<Mail size={16} />}
                        value={email}
                        maxLength={MAX_LENGTHS.INPUT}
                        placeholder="Enter admin's email"
                        required
                        onChange={(e) => setEmail(e.target.value)}
                    />

                    <Input
                        label="Password"
                        type={showPassword ? 'text' : 'password'}
                        icon={<Lock size={16} />}
                        minLength={8}
                        maxLength={100}
                        value={password}
                        placeholder="Enter admin's password"
                        required
                        onChange={(e) => setPassword(e.target.value)}
                        rightIcon={showPassword ? <Eye size={16} /> : <EyeOff size={16} />}
                        onRightIconClick={() => setShowPassword((s) => !s)}
                    />

                    <Button type="submit" fullWidth>Add admin</Button>
                </Card>
            </form>

            <ConfirmDialog
                open={confirmOpen}
                onClose={() => setConfirmOpen(false)}
                onConfirm={handleCreate}
                title="Confirm admin creation?"
                message={`Are you sure you want to create the admin account for ${name}?`}
                confirmLabel="Yes, create admin"
                loading={submitting}
            />
        </div>
    );
}
