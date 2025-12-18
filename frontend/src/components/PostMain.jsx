import { useEffect, useRef, useState } from 'react';
import { IoIosPerson } from 'react-icons/io';
import { BsThreeDots } from 'react-icons/bs';
import { FaRegComment } from 'react-icons/fa';
import { IoClose } from 'react-icons/io5';
import VoteWidget from './VoteWidget';
import './style/postFullPage.css';

const PostMain = ({ post, user, onEdit, onDelete }) => {
    const [postOptions, setPostOptions] = useState(false);
    const [openImage, setOpenImage] = useState(false);
    const menuRef = useRef(null);

    const ownerIdConverted = post?.ownerId ? parseInt(post.ownerId, 10) : null;
    const postId = post?.id || post?.postId;

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (menuRef.current && !menuRef.current.contains(event.target)) {
                setPostOptions(false);
            }
        };

        if (postOptions) {
            document.addEventListener('mousedown', handleClickOutside);
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [postOptions]);

    const viewerMenu = [
        { label: 'Follow', onClick: () => console.log('Follow clicked') }
    ];
    
    const authorMenu = [
        { label: 'Edit', onClick: onEdit },
        { label: 'Delete', onClick: onDelete }
    ];

    return (
        <article className="post-card">
            <div className="post-header">
                <div className="user-profile-pic">
                    {post?.avatar ? post.avatar : <IoIosPerson />}
                </div>
                <div className="user-info">
                    <h2 className="user-name">{user?.id}</h2>
                    <time dateTime={post?.time}>{post?.time}</time>
                </div>
                <div className="post-settings" ref={menuRef}>
                    {ownerIdConverted !== null && user?.id !== undefined && ownerIdConverted === user.id && (
                        <>
                            <BsThreeDots onClick={() => setPostOptions(prev => !prev)}/>
                            {postOptions && (
                                <div className="options-menu">
                                    <ul>
                                        {(ownerIdConverted === user?.id ? authorMenu : viewerMenu).map((item, index) => (
                                            <li key={index} onClick={item.onClick}>{item.label}</li>
                                        ))}
                                    </ul>
                                </div>
                            )}
                        </>
                    )}
                </div>
            </div>
            <div className="post-content">
                <div className="post-title" >
                    <p>{post?.title}</p>
                </div>
                <div className="post-media" >
                    {post?.media && <img src={post.media} alt={post.title || 'Post content'} onClick={() => setOpenImage(true)}/>}
                    {post?.content && <p className="post-text">{post.content}</p>}
                </div>
            </div>
            <footer className="post-footer">
                <VoteWidget
                    targetId={postId}
                    initialUp={post?.upvoteCount || 0}
                    initialDown={post?.downvoteCount || 0}
                    isPost={true}
                    onChange={() => {}}
                />
                <div className="post-comment">
                    <FaRegComment />
                    <span className="comment-count">{post?.commentCount || 0}</span>
                </div>
            </footer>
            {openImage && post?.media && (
                <div className="view-image-container" onClick={() => setOpenImage(false)}>
                    <div className="view-image">
                        <IoClose className="close-button" onClick={() => setOpenImage(false)} />
                        <img src={post.media} alt={post.title || 'Post content'} onClick={(e) => e.stopPropagation()} />
                    </div>
                </div>
            )}
        </article>
    );
};

export default PostMain;
