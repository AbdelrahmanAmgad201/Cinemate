import { useState, useEffect, useRef, useContext } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import pic from "../../assets/action.jpg";
import { AuthContext } from '../../context/AuthContext';
import EditPost from '../../components/EditPost';
import { updatePostApi, deletePostApi } from '../../api/post-api';
import { addCommentApi, getPostCommentsApi, deleteCommentApi, getRepliesApi } from '../../api/comment-api';
import VoteWidget from '../../components/VoteWidget';
import "../../components/style/postCard.css";
import "../../components/style/postFullPage.css";
import { IoIosPerson } from "react-icons/io";
import { BsThreeDots } from "react-icons/bs";
import { BiUpvote, BiDownvote, BiSolidUpvote, BiSolidDownvote } from "react-icons/bi";
import { RiShareForwardLine } from "react-icons/ri";
import { FaRegComment } from "react-icons/fa";
import { MdKeyboardArrowDown } from "react-icons/md";
import { IoClose } from "react-icons/io5";
import { isVoted } from '../../api/vote-api';

const CommentItem = ({ comment, onVoteUpdate, onRemoveComment, onEdit, postOwnerId, onPostCommentAdded, onIncrementTopLevelReplies }) => {
    const { user } = useContext(AuthContext);
    const [menuOpen, setMenuOpen] = useState(false);
    const menuRef = useRef(null);

    useEffect(() => {
        const handleClickOutside = (e) => {
            if (menuRef.current && !menuRef.current.contains(e.target)) {
                setMenuOpen(false);
            }
        };
        if (menuOpen) document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [menuOpen]);

    const isCommentOwner = parseInt(comment.ownerId, 16) === user.id;
    const isPostOwner = postOwnerId ? parseInt(postOwnerId, 16) === user.id : false;
    const canDelete = isCommentOwner || isPostOwner;

    const handleDeleteClick = async () => {
        if (!window.confirm('Delete this comment?')) return;
        try {
            const res = await deleteCommentApi({ commentId: comment.id });
            if (res.success) {
                onRemoveComment && onRemoveComment(comment.id);
            } else {
                console.error('Failed to delete comment:', res.message);
            }
        } catch (e) {
            console.error('Error deleting comment:', e);
        }
    };

    // editing removed: backend does not support persistent comment edits
    const [showReplyBox, setShowReplyBox] = useState(false);
    const [replyText, setReplyText] = useState("");
    const [submittingReply, setSubmittingReply] = useState(false);
    const [showReplies, setShowReplies] = useState(false);
    const [replies, setReplies] = useState(comment.replies || []);
    const [repliesLoading, setRepliesLoading] = useState(false);

    const loadReplies = async () => {
        if (!comment.id) return;
        setRepliesLoading(true);
        try {
            const sortBy = 'score';
            const res = await getRepliesApi({ parentId: comment.id, sortBy });
            if (res.success) {
                const list = res.data || [];
                // preserve client-side createdAt when present and keep optimistic local replies that server hasn't returned yet
                const prevById = Object.fromEntries((replies || []).map(r => [r.id, r]));
                const merged = list.map(r => ({ ...r, createdAt: r.createdAt || prevById[r.id]?.createdAt }));
                // append any local-only replies that the server didn't return (optimistic responses)
                Object.values(prevById).forEach(p => {
                    if (!merged.find(m => m.id === p.id)) merged.push(p);
                });
                setReplies(merged);
            }
        } catch (e) {
            console.error('Error loading replies:', e);
        } finally {
            setRepliesLoading(false);
        }
    };

    const handleViewReplies = async () => {
        if (showReplies) {
            setShowReplies(false);
            return;
        }
        if ((replies || []).length === 0) await loadReplies();
        setShowReplies(true);
    };

    const handleReplySubmit = async () => {
        if (!replyText.trim() || submittingReply) return;
        setSubmittingReply(true);
        try {
            const res = await addCommentApi({ postId: comment.postId, parentId: comment.id, content: replyText.trim() });
            if (res.success) {
                const nowIso = new Date().toISOString();
                const ownerHex = user?.id ? user.id.toString(16).padStart(24, '0') : '0'.padStart(24, '0');
                const newReply = {
                    id: res.data,
                    ownerId: ownerHex,
                    postId: comment.postId,
                    parentId: comment.id,
                    content: replyText.trim(),
                    upvoteCount: 0,
                    downvoteCount: 0,
                    score: 0,
                    depth: comment.depth + 1,
                    numberOfReplies: 0,
                    createdAt: nowIso
                };
                setReplyText("");
                // attach reply locally
                setReplies(prev => [newReply, ...(prev || [])]);
                setShowReplies(true);
                // inform parent to increment post comment count
                onPostCommentAdded && onPostCommentAdded();
                // increment parent numberOfReplies in parent comments list
                onEdit && onEdit({ commentId: comment.id, numberOfReplies: (comment.numberOfReplies || 0) + 1 });
                // increment top-level comment's numberOfReplies as well so main comment shows "View X replies"
                if (typeof onIncrementTopLevelReplies === 'function') onIncrementTopLevelReplies(comment.id, 1);
            } else {
                console.error('Failed to post reply:', res.message);
            }
        } catch (e) {
            console.error('Error posting reply:', e);
        } finally {
            setSubmittingReply(false);
        }
    };

    return (
        <div className="comment-item">
            <VoteWidget
                targetId={comment.id}
                initialUp={comment.upvoteCount}
                initialDown={comment.downvoteCount}
                isPost={false}
                onChange={onVoteUpdate}
            />
            <div className="comment-main-content">
                <div className="comment-header" style={{ position: 'relative' }}>
                    <div className="comment-avatar">
                        <IoIosPerson />
                    </div>
                    <span className="comment-author">User {parseInt(comment.ownerId, 16)}</span>
                    <time className="comment-time" dateTime={comment.createdAt || new Date().toISOString()}>
                        {comment.createdAt ? new Date(comment.createdAt).toLocaleString() : new Date().toLocaleString()}
                    </time>
                    {(isCommentOwner || canDelete) && (
                        <div className="comment-settings" ref={menuRef}>
                            <BsThreeDots onClick={() => setMenuOpen(prev => !prev)} />
                            {menuOpen && (
                                <div className="options-menu">
                                    <ul>
                                        {canDelete && (
                                            <li onClick={handleDeleteClick}>Delete</li>
                                        )}
                                    </ul>
                                </div>
                            )}
                        </div>
                    )}
                </div>
                <div className="comment-content">
                    <p>{comment.content}</p>
                </div>
                <div className="comment-actions">
                    <button className="reply-btn" onClick={() => setShowReplyBox(prev => !prev)}>Reply</button>
                    { (comment.numberOfReplies || 0) > 0 && (
                        <button className="view-replies-btn" onClick={handleViewReplies}>{showReplies ? 'Hide' : `View ${comment.numberOfReplies} replies`}</button>
                    ) }
                </div>
                {showReplyBox && (
                    <div className="reply-box">
                        <textarea
                            value={replyText}
                            onChange={(e) => setReplyText(e.target.value)}
                            placeholder="Write a reply..."
                        />
                        <div className="reply-actions">
                            <button className="comment-btn" onClick={handleReplySubmit} disabled={!replyText.trim() || submittingReply}>{submittingReply ? 'Replying...' : 'Reply'}</button>
                            <button className="comment-btn" onClick={() => setShowReplyBox(false)}>Cancel</button>
                        </div>
                    </div>
                )}
                {showReplies && (
                    <div className="comment-replies">
                        {repliesLoading ? <div className="loading-comments">Loading replies...</div> : (
                            replies.map(r => (
                                <CommentItem key={r.id} comment={r} postOwnerId={postOwnerId}
                                    onVoteUpdate={onVoteUpdate}
                                    onRemoveComment={(rid) => {
                                        // bubble delete up to parent handler
                                        onRemoveComment && onRemoveComment(rid);
                                        // remove locally
                                        setReplies(prev => prev.filter(x => x.id !== rid));
                                    }}
                                    onEdit={({ commentId, content, numberOfReplies }) => setReplies(prev => prev.map(x => x.id === commentId ? { ...x, ...(content !== undefined ? { content } : {}), ...(numberOfReplies !== undefined ? { numberOfReplies } : {}) } : x))}
                                    onPostCommentAdded={onPostCommentAdded}
                                    onIncrementTopLevelReplies={onIncrementTopLevelReplies}
                                />
                            ))
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

const PostFullPage = () => {
    const { postId } = useParams();
    const location = useLocation();
    const { user } = useContext(AuthContext);
    const navigate = useNavigate();
    const menuRef = useRef(null);

    const [post, setPost] = useState(location.state?.post || null);
    const [openImage, setOpenImage] = useState(false);
    // post vote handled by VoteWidget
    
    const [editMode, setEditMode] = useState(false);
    const [sort, setSort] = useState("best");
    const [commentText, setCommentText] = useState("");
    const [postOptions, setPostOptions] = useState(false);
    const [isSubmittingComment, setIsSubmittingComment] = useState(false);
    const [comments, setComments] = useState([]);
    const [commentsLoading, setCommentsLoading] = useState(false);
    const ownerIdConverted = post?.ownerId ? parseInt(post.ownerId, 10) : null;

    const handleVote = async (voteType) => {
        const previousVote = userVote;
        const newVote = userVote === voteType ? 0 : voteType;
        
        const voteDifference = newVote - previousVote;
        
        setUserVote(newVote);
        setVoteCount(prevCount => prevCount + voteDifference);

        try{
            let result;
            if(previousVote === 0 && newVote !== 0){
                result = await votePostApi({ postId: postId, value: newVote });
                if (result.success) {
                    console.log("Vote created");
                }
            }
            else if (newVote === 0 && previousVote !== 0){
                result = await deleteVotePostApi({ targetId: postId });
                
                
                if (result.success) {
                    console.log("Vote deleted");
                }
            }

            else if (previousVote !== 0 && newVote !== 0) {
                result = await updateVotePostApi({ postId: postId, value: newVote });
            }
            if (!result?.success) {
                setUserVote(previousVote);
                setVoteCount(prevCount => prevCount - voteDifference);
                console.error('Vote failed:', result?.message);
            }
        }
        catch(e){
            setUserVote(previousVote);
            setVoteCount(prevCount => prevCount - voteDifference);
            console.error('Vote error:', e);
        }
    };

    const handleCommentSubmit = async () => {
        if (!commentText.trim() || isSubmittingComment) {
            return;
        }

        setIsSubmittingComment(true);
        try {
            const result = await addCommentApi({
                postId: post.id,
                parentId: null,
                content: commentText.trim()
            });

            if (result.success) {
                console.log('Comment posted successfully:', result.data);
                setCommentText("");
                // Optimistically increment post comment count and reload comments
                setPost(prev => prev ? { ...prev, commentCount: (prev.commentCount || 0) + 1 } : prev);
                loadComments();
            } else {
                console.error('Failed to post comment:', result.message);
            }
        } catch (error) {
            console.error('Error posting comment:', error);
        } finally {
            setIsSubmittingComment(false);
        }
    };

    const loadComments = async () => {
        if (!post?.id) return;
        
        setCommentsLoading(true);
        try {
            const sortBy = sort === 'best' ? 'score' : 'new';
            const result = await getPostCommentsApi({
                postId: post.id,
                page: 0,
                size: 20,
                sortBy
            });

            if (result.success && result.data?.content) {
                let loaded = result.data.content;
                if (sort === 'best') {
                    loaded = [...loaded].sort((a, b) => {
                        const scoreA = (a.upvoteCount || 0) - (a.downvoteCount || 0);
                        const scoreB = (b.upvoteCount || 0) - (b.downvoteCount || 0);
                        if (scoreB !== scoreA) return scoreB - scoreA;
                        // tie-breaker: newest first
                        return new Date(b.createdAt || 0) - new Date(a.createdAt || 0);
                    });
                } else if (sort === 'new') {
                    loaded = [...loaded].sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0));
                }
                // Attach replies for comments that have replies so they persist after reload
                // Some backends may not persist `numberOfReplies`; as a fallback, fetch replies for
                // the first few comments so replies don't disappear after reload.
                const parentsWithReplies = loaded.filter(c => (c.numberOfReplies || 0) > 0);
                // fallback candidates (first 5) if no counts were found
                const candidates = (parentsWithReplies.length > 0 ? parentsWithReplies : loaded.slice(0, 5)).slice(0, 10);
                if (candidates.length > 0) {
                    const fetchRecursive = async (parentId, depth) => {
                        const res = await getRepliesApi({ parentId, sortBy: 'score' });
                        if (!res.success) return [];
                        const list = res.data || [];
                        if (depth > 1) {
                            await Promise.all(list.map(async (r) => {
                                if ((r.numberOfReplies || 0) > 0) {
                                    r.replies = await fetchRecursive(r.id, depth - 1);
                                }
                            }));
                        }
                        return list;
                    };

                    const loadedWithReplies = await Promise.all(loaded.map(async c => {
                        if (candidates.find(x => x.id === c.id)) {
                            const replies = await fetchRecursive(c.id, 2);
                            return { ...c, replies, numberOfReplies: Math.max(c.numberOfReplies || 0, replies.length) };
                        }
                        return c;
                    }));
                    // compute total comments including replies (recursively)
                    const countRepliesRecursive = (arr) => {
                        return (arr || []).reduce((sum, it) => sum + 1 + countRepliesRecursive(it.replies || []), 0);
                    };
                    const totalComments = loadedWithReplies.reduce((sum, it) => sum + 1 + countRepliesRecursive(it.replies || []), 0);
                    setComments(loadedWithReplies);
                    setPost(prev => prev ? { ...prev, commentCount: totalComments } : prev);
                } else {
                    setComments(loaded);
                    const totalComments = loaded.reduce((sum, it) => sum + 1, 0);
                    setPost(prev => prev ? { ...prev, commentCount: Math.max(prev.commentCount || 0, totalComments) } : prev);
                }
            }
        } catch (error) {
            console.error('Error loading comments:', error);
        } finally {
            setCommentsLoading(false);
        }
    };

    // Find and increment numberOfReplies for the top-level comment that contains targetId in its subtree
    const incrementTopLevelRepliesFor = (targetId, delta = 1) => {
        setComments(prev => {
            if (!prev) return prev;
            const updated = prev.map(c => {
                // quick check: if top-level comment is the target
                if (c.id === targetId) {
                    return { ...c, numberOfReplies: (c.numberOfReplies || 0) + delta };
                }
                // otherwise, search in loaded replies recursively
                const found = findInReplies(c.replies || [], targetId);
                if (found) {
                    return { ...c, numberOfReplies: (c.numberOfReplies || 0) + delta };
                }
                return c;
            });
            return updated;
        });
    };

    const findInReplies = (repliesArr, targetId) => {
        for (const r of repliesArr || []) {
            if (r.id === targetId) return true;
            if (r.replies && findInReplies(r.replies, targetId)) return true;
        }
        return false;
    };

    const handleDelete = async () => {
        if (!window.confirm('Are you sure you want to delete this post?')) {
            return;
        }

        setPostOptions(false);

        try{
            const result = await deletePostApi({
                postId: post.postId
            });

            if(result.success){
                console.log('Post deleted successfully');
                navigate(`/forum/${post.forumId}`);
            }

            else{
                console.log(result.message || 'Failed to delete post');
            }
        }
        catch(error){
            console.error('Error delete post:', error);
        } 

    }

    const handleEdit = () => {
        setEditMode(true);
        setPostOptions(false);
    };

    const cancelEdit = () => {
        setEditMode(false);
    };

    const saveEdit = async (updatedPost, mediaFile) => {
        try {
            const result = await updatePostApi({
                postId: post.id, 
                forumId: post.forumId,
                title: updatedPost.title,
                content: updatedPost.content
            });
    
            if (result.success) {
                // Update the local post state with new data
                setPost({
                    ...post,
                    title: updatedPost.title,
                    content: updatedPost.content,
                    media: updatedPost.media
                });
                setEditMode(false);
                console.log('Post updated successfully');
            } else {
                console.error('Update failed:', result.message);
            }
        } catch (error) {
            console.error('Error updating post:', error);
        }
    };





    useEffect(() => {
        if (location.state?.post) {
            return;
        }
    }, [postId, location.state]);

    useEffect(() => {
        if (location.state?.editMode) {
            setEditMode(true);
            navigate(location.pathname, { replace: true, state: {} });
        }
    }, [postId, location.state, location.pathname, navigate]);

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

    useEffect(() => {
        loadComments();
    }, [post?.id, sort]);




    const viewerMenu = [
        { label: "Follow", onClick: () => console.log("Follow clicked") }
    ];
    
    const authorMenu = [
        { label: "Edit", onClick: handleEdit },
        { label: "Delete", onClick: handleDelete }
    ];

    if (!post) {
        return <div>Loading...</div>;
    }

    if(editMode){
        return (
            <div className="post-page">
                <EditPost post={post} onSave={saveEdit} onCancel={cancelEdit} />
            </div>
        );
    }

    return (
        <div className="post-page">
            <article className="post-card">
                <div className="post-header">
                    <div className="user-profile-pic">
                        {post.avatar ? post.avatar : <IoIosPerson />}
                    </div>
                    <div className="user-info">
                        <h2 className="user-name">{user.id}</h2>
                        <time dateTime={post.time}>{post.time}</time>
                    </div>
                    <div className="post-settings" ref={menuRef}>
                        {ownerIdConverted === user.id && ( 
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
                        <p>{post.title}</p>
                    </div>
                    <div className="post-media" >
                        {post.media && <img src={post.media} alt={post.title || "Post content"} onClick={() => setOpenImage(true)}/>}
                        {post.content && <p className="post-text">{post.content}</p>}
                    </div>
                </div>
                <footer className="post-footer">
                            <VoteWidget
                            targetId={postId}
                            initialUp={post?.upvoteCount || 0}
                            initialDown={post?.downvoteCount || 0}
                            isPost={true}
                            onChange={() => {
                                // refresh post to pick up updated counts
                                try {
                                    // if backend has endpoint to refetch post, we can call it; for now update local post by reloading page or leave
                                } catch (e) {
                                    console.error('Error refreshing post after vote');
                                }
                            }}
                        />
                    <div className="post-comment">
                        <FaRegComment />
                        <span className="comment-count">{post.commentCount || 0}</span>
                    </div>
                    {/* <div className="post-share">
                        <RiShareForwardLine />
                    </div> */}
                </footer>
            </article>
            {openImage && (
                <div className="view-image-container" onClick={() => setOpenImage(false)}>
                    <div className="view-image">
                        <IoClose className="close-button" onClick={() => setOpenImage(false)} />
                        <img src={post.media} alt={post.title || "Post content"} onClick={(e) => e.stopPropagation()} />
                    </div>
                </div>
            )}
            <div className="comment-input">
                <textarea 
                    value={commentText} 
                    onChange={(e) => setCommentText(e.target.value)} 
                    placeholder="Share your thoughts"
                    disabled={isSubmittingComment}
                />
                <button 
                    className="comment-btn" 
                    onClick={handleCommentSubmit}
                    disabled={!commentText.trim() || isSubmittingComment}
                >
                    {isSubmittingComment ? 'Posting...' : 'Comment'}
                </button>
            </div>
            <div className="comments-section">
                {/* <h3>Comments</h3> */}
                <div className="comments-sort">
                    <label htmlFor="sort-by">Sort by:</label>
                    <div className="select-wrapper">
                        <select id="sort-by" name="sort-by" value={sort} onChange={(e) => setSort(e.target.value)}>
                            <option value="best">Best</option>
                            <option value="new">New</option>
                        </select>
                        <MdKeyboardArrowDown className="select-arrow" />
                    </div>
                </div>
                {commentsLoading ? (
                    <p className="loading-comments">Loading comments...</p>
                ) : comments.length > 0 ? (
                    <div className="comments-list">
                        {comments.map((comment) => (
                            <CommentItem
                                key={comment.id}
                                comment={comment}
                                postOwnerId={post?.ownerId}
                                onPostCommentAdded={() => setPost(prev => prev ? { ...prev, commentCount: (prev.commentCount || 0) + 1 } : prev)}
                                onIncrementTopLevelReplies={(id, delta) => incrementTopLevelRepliesFor(id, delta)}
                                onVoteUpdate={(payload) => {
                                    // payload: { targetId, previousVote, newVote }
                                    if (!payload || payload.targetId !== comment.id) return;
                                    const prev = payload.previousVote || 0;
                                    const next = payload.newVote || 0;
                                    const prevUp = prev === 1 ? 1 : 0;
                                    const prevDown = prev === -1 ? 1 : 0;
                                    const newUp = next === 1 ? 1 : 0;
                                    const newDown = next === -1 ? 1 : 0;
                                    const upDelta = newUp - prevUp;
                                    const downDelta = newDown - prevDown;
                                    setComments(prevComments => {
                                        const updated = prevComments.map(c => {
                                            if (c.id !== payload.targetId) return c;
                                            return {
                                                ...c,
                                                upvoteCount: (c.upvoteCount || 0) + upDelta,
                                                downvoteCount: (c.downvoteCount || 0) + downDelta,
                                            };
                                        });
                                        if (sort === 'best') {
                                            return [...updated].sort((a, b) => {
                                                const scoreA = (a.upvoteCount || 0) - (a.downvoteCount || 0);
                                                const scoreB = (b.upvoteCount || 0) - (b.downvoteCount || 0);
                                                if (scoreB !== scoreA) return scoreB - scoreA;
                                                return new Date(b.createdAt || 0) - new Date(a.createdAt || 0);
                                            });
                                        }
                                        if (sort === 'new') {
                                            return [...updated].sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0));
                                        }
                                        return updated;
                                    });
                                }}
                                onRemoveComment={(commentId) => {
                                    setComments(prev => {
                                        let removedTop = false;
                                        let removedNested = false;
                                        const filtered = prev.filter(c => {
                                            if (c.id === commentId) {
                                                removedTop = true;
                                                return false;
                                            }
                                            return true;
                                        });
                                        if (!removedTop) {
                                            const mapped = filtered.map(c => {
                                                const prevReplies = c.replies || [];
                                                const newReplies = prevReplies.filter(r => r.id !== commentId);
                                                if (newReplies.length !== prevReplies.length) {
                                                    removedNested = true;
                                                    return { ...c, replies: newReplies, numberOfReplies: Math.max((c.numberOfReplies || 1) - 1, 0) };
                                                }
                                                return c;
                                            });
                                            return mapped;
                                        }
                                        return filtered;
                                    });
                                    setPost(prev => prev ? { ...prev, commentCount: Math.max((prev.commentCount || 1) - 1, 0) } : prev);
                                }}
                                onEdit={({ commentId, content, numberOfReplies }) => setComments(prev => prev.map(c => c.id === commentId ? { ...c, ...(content !== undefined ? { content } : {}), ...(numberOfReplies !== undefined ? { numberOfReplies } : {}) } : c))}
                            />
                        ))}
                    </div>
                ) : (
                    <p className="no-comments">No comments yet. Be the first to comment!</p>
                )}
            </div>
        </div>
    );
};

export default PostFullPage;