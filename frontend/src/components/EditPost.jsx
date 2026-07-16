import { useState, useRef } from 'react';
import { X, Trash2, ImagePlus } from 'lucide-react';
import ConfirmDialog from './ui/ConfirmDialog.jsx';
import Button from './ui/Button.jsx';
import './style/editPost.css';

const EditPost = ({ post, onSave, onCancel }) => {
    const [editedTitle, setEditedTitle] = useState(post.title);
    const [editedText, setEditedText] = useState(post.content);
    const [addedMedia, setAddedMedia] = useState(post.media);
    const [mediaFile, setMediaFile] = useState(null);
    const [errors, setErrors] = useState({});
    const [confirmDiscardOpen, setConfirmDiscardOpen] = useState(false);

    const fileInputRef = useRef(null);

    function uploadMedia(e) {
        const file = e.target.files[0];
        if (file) {
            if (file.size > 5 * 1024 * 1024) {
                setErrors((prev) => ({ ...prev, media: 'Image must be less than 5MB' }));
                return;
            }

            if (!file.type.startsWith('image/')) {
                setErrors((prev) => ({ ...prev, media: 'File must be an image' }));
                return;
            }

            const reader = new FileReader();
            reader.onloadend = () => {
                setAddedMedia(reader.result);
                setMediaFile(file);
                setErrors((prev) => ({ ...prev, media: null }));
            };
            reader.readAsDataURL(file);
        }
    }

    const removeMedia = () => {
        setAddedMedia(null);
        setMediaFile(null);
        if (fileInputRef.current) fileInputRef.current.value = '';
    };

    const handleSave = async () => {
        const updatedPost = {
            ...post,
            title: editedTitle.trim(),
            content: editedText.trim(),
            text: editedText.trim(),
            media: addedMedia,
        };
        onSave(updatedPost, mediaFile);
    };

    return (
        <div className="edit-post-container">
            <div className="edit-post-header">
                <button type="button" className="close-icon" onClick={() => setConfirmDiscardOpen(true)} aria-label="Discard changes">
                    <X size={20} />
                </button>
            </div>

            <div className="edit-post-form">
                <div className="edit-input">
                    <input type="text" id="edit-title" value={editedTitle} onChange={(e) => setEditedTitle(e.target.value)} maxLength={100} />
                    <span className="char-count">{editedTitle.length}/100</span>
                </div>
                <div className="edit-input">
                    {addedMedia ? (
                        <div className="added-media-container">
                            <img src={addedMedia} alt="" className="added-media" />
                            <button type="button" className="remove-media" onClick={removeMedia}>
                                <Trash2 size={16} /> Delete image
                            </button>
                        </div>
                    ) : (
                        <div className="upload-media-container">
                            <input type="file" id="upload-media" hidden ref={fileInputRef} accept="image/*" onChange={uploadMedia} />
                            <label htmlFor="upload-media" className="upload-button">
                                <ImagePlus size={16} /> Upload image
                            </label>
                            {errors.media && <span className="edit-post-error">{errors.media}</span>}
                        </div>
                    )}
                </div>
                <div className="edit-input">
                    <textarea id="text" value={editedText} onChange={(e) => setEditedText(e.target.value)} maxLength="10000" />
                    <span className="char-count">{editedText.length}/10000</span>
                </div>
                <div className="edit-actions">
                    <Button variant="ghost" onClick={() => setConfirmDiscardOpen(true)}>Cancel</Button>
                    <Button onClick={handleSave}>Save changes</Button>
                </div>
            </div>

            <ConfirmDialog
                open={confirmDiscardOpen}
                onClose={() => setConfirmDiscardOpen(false)}
                onConfirm={() => { setConfirmDiscardOpen(false); onCancel(); }}
                title="Discard changes?"
                message="Your edits will be lost."
                confirmLabel="Discard"
                danger
            />
        </div>
    );
};

export default EditPost;
