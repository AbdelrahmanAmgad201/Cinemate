import { useContext, useEffect, useState } from 'react';
import { AuthContext } from '../../context/AuthContext.jsx';
import { useNavigate, useParams } from 'react-router-dom';
import { ToastContext } from '../../context/ToastContext.jsx';
import './style/Mod.css';
import { PATHS } from '../../constants/constants.jsx';
import { updateForumApi, deleteForumApi, getForumApi } from '../../api/forum-api.js';
import Avatar from '../../components/ui/Avatar.jsx';
import Input from '../../components/ui/Input.jsx';
import Textarea from '../../components/ui/Textarea.jsx';
import Button from '../../components/ui/Button.jsx';
import ConfirmDialog from '../../components/ui/ConfirmDialog.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import { ShieldOff } from 'lucide-react';

export default function Mod() {
    const { user } = useContext(AuthContext);
    const { showToast } = useContext(ToastContext);
    const navigate = useNavigate();
    const { forumId } = useParams();

    const [modId, setModId] = useState();
    const isMod = modId === user.id;

    const [forumName, setForumName] = useState('');
    const [forumDescription, setForumDescription] = useState('');
    const [saving, setSaving] = useState(false);
    const [deleted, setDeleted] = useState(false);
    const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);

    const handleSave = async (e) => {
        e.preventDefault();
        setSaving(true);

        const res = await updateForumApi({ forumId, name: forumName, description: forumDescription });

        setSaving(false);

        if (!res.success) return showToast('Failed to update forum', res.message || 'unknown error', 'error');

        showToast('Saved', 'Forum data updated successfully', 'success');
    };

    const handleDeleteForum = async () => {
        setConfirmDeleteOpen(false);

        const res = await deleteForumApi({ forumId });

        if (!res.success) return showToast('Failed to delete forum', res.message || 'unknown error', 'error');

        setDeleted(true);
        showToast('Deleted', 'Forum marked deleted', 'info');

        setTimeout(() => navigate(PATHS.HOME), 2000);
    };

    useEffect(() => {
        const fetchForumDetails = async () => {
            const res = await getForumApi({ forumId });

            if (!res.success) {
                showToast('Failed to fetch forum details', res.message || 'unknown error', 'error');
                navigate(PATHS.HOME);
                return;
            }

            setForumName(res.data.name);
            setForumDescription(res.data.description);
            setModId(res.data.ownerId);
        };

        if (forumId) fetchForumDetails();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [forumId]);

    if (deleted) {
        return <EmptyState title="Forum deleted" description="This forum has been removed." />;
    }

    if (!isMod) {
        return <EmptyState icon={<ShieldOff size={28} />} title="You are not a moderator" description="Only this forum's moderator can access these tools." />;
    }

    return (
        <div className="mod-container">
            <h1 className="mod-page-title">Moderator tools</h1>

            <div className="mod-forum-summary">
                <Avatar name={forumName} size="md" />
                <h2>{forumName}</h2>
            </div>

            <section className="mod-section">
                <h2 className="section-title">Forum settings</h2>
                <form className="mod-form" onSubmit={handleSave}>
                    <Input
                        label="Forum name"
                        value={forumName}
                        onChange={(e) => setForumName(e.target.value)}
                    />
                    <Textarea
                        label="Description"
                        value={forumDescription}
                        onChange={(e) => setForumDescription(e.target.value)}
                        rows={4}
                    />
                    <div className="form-actions">
                        <Button type="submit" loading={saving}>Save changes</Button>
                        <Button type="button" variant="danger" onClick={() => setConfirmDeleteOpen(true)}>Delete forum</Button>
                    </div>
                </form>
            </section>

            <ConfirmDialog
                open={confirmDeleteOpen}
                onClose={() => setConfirmDeleteOpen(false)}
                onConfirm={handleDeleteForum}
                title="Delete forum?"
                message="This will permanently delete the forum and cannot be undone."
                confirmLabel="Delete forum"
                danger
            />
        </div>
    );
}
