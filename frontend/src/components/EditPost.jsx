import React, { useState, useRef, useContext } from 'react';
import { updatePostApi } from '../api/post-api';
import { IoClose } from "react-icons/io5";
import { MdDelete } from "react-icons/md";
import { FaFileImage } from "react-icons/fa";
import Swal from "sweetalert2";
import { ToastContext } from '../context/ToastContext';
import "./style/editPost.css";

const EditPost = ({ post, onSave, onCancel }) => {

    const [editedTitle, setEditedTitle] = useState(post.title);
    const [editedText, setEditedText] = useState(post.content);
    const [addedMedia, setAddedMedia] = useState(post.media);
    const [mediaFile, setMediaFile] = useState(null);
    const [loading, setLoading] = useState(false);
    const [errors, setErrors] = useState({});

    
    const fileInputRef = useRef(null);
    const { showToast } = useContext(ToastContext);

    function uploadMedia(e) {
        const file = e.target.files[0];
        if(file){
            if(file.size > 5 * 1024 * 1024){        //5MB Max
                setErrors(prev => ({ ...prev, media: 'Image must be less than 5MB' }));
                return;
            }

            if (!file.type.startsWith('image/')) {
                setErrors(prev => ({ ...prev, media: 'File must be an image' }));
                return;
            }

            const reader = new FileReader();
            reader.onloadend = () => {
                setAddedMedia(reader.result);
                setMediaFile(file);
                setErrors(prev => ({ ...prev, media: null }));
            };
            reader.readAsDataURL(file);
        }
    };

    const removeMedia = () => {
        setAddedMedia(null);
        setMediaFile(null);
        if(fileInputRef.current){
            fileInputRef.current.value = "";
        }
    };

    const handleCancel = async () => {
        const result = await Swal.fire({
            title: 'Discard changes?',
            showCancelButton: true,
            confirmButtonText: 'Discard',
            confirmButtonColor: '#d33',
            cancelButtonText: 'Cancel',

        });
        if (!result.isConfirmed) return;

        onCancel();
        
    };

    const handleSave = async () => {
        
        const updatedPost = {
            ...post,
            title: editedTitle.trim(),
            content: editedText.trim(),
            text: editedText.trim(),
            media: addedMedia
        };
       onSave(updatedPost, mediaFile);
            
    };


    return(
        <div className="edit-post-container">
            <div className="edit-post-header">
                <IoClose className="close-icon" onClick={handleCancel} />
            </div>

            <div className="edit-post-form">
                <div className="edit-input">
                    <input type="text" id="edit-title" value={editedTitle} onChange={(e) => setEditedTitle(e.target.value)} maxLength={100} disabled={loading} />
                    <span className="char-count">{editedTitle.length}/100</span>
                </div>
                <div className="edit-input">
                    {addedMedia ? (
                        <div className="added-media-container">
                            <img src={addedMedia} className="added-media" />
                            <div className="remove-media" onClick={removeMedia}>
                                <MdDelete /> Delete image
                            </div>
                        </div>
                    ) : (
                        <div className="upload-media-container">
                            <input type="file" id="upload-media" hidden ref={fileInputRef} accept="image/*" onChange={uploadMedia} />
                            <label htmlFor="upload-media" className="upload-button">
                                <FaFileImage /> Upload image
                            </label>
                        </div>
                    )}
                </div>
                <div className="edit-input">
                    <textarea id="text" value={editedText} onChange={(e) => setEditedText(e.target.value)} maxLength="10000" />
                    <span className="char-count">{editedText.length}/10000</span>
                </div>
                <div className="edit-actions">
                    <button className="cancel-button" onClick={handleCancel}>
                        Cancel
                    </button>
                    <button className="save-button" onClick={handleSave}>
                        Save Changes
                    </button>
                </div>
            </div>
        </div>
    );
};

export default EditPost;