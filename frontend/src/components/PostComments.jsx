import { useState, useEffect, useRef, useContext, useCallback } from 'react';
// Simple cache for user names
const userNameCache = {};
import { getModApi } from '../api/forum-api';
// Simple cache for user names. Use forum API helper so we get axios + auth handling.
async function fetchUserNameById(userId) {
    if (!userId) return null;
    if (userNameCache[userId]) return userNameCache[userId];
    try {
        console.log('[UserNameLookup] Requesting name for userId via getModApi:', userId);
        const res = await getModApi({ userId });
        if (!res.success) {
            console.warn('[UserNameLookup] getModApi failed for', userId, res.message);
            return null;
        }
        const text = res.data;
        console.log('[UserNameLookup] Response for userId', userId, ':', text);
        if (text && text !== 'Unknown user' && typeof text === 'string' && !text.trim().toLowerCase().startsWith('<!doctype html>')) {
            userNameCache[userId] = text;
            return text;
        }
        return null;
    } catch (e) {
        console.error('[UserNameLookup] Error for userId', userId, e);
        return null;
    }
}
import { Link } from 'react-router-dom';
import { IoIosPerson } from 'react-icons/io';
import { BsThreeDots } from 'react-icons/bs';
import { MdKeyboardArrowDown } from 'react-icons/md';
import { addCommentApi, getPostCommentsApi, deleteCommentApi, getRepliesApi } from '../api/comment-api';
import VoteWidget from './VoteWidget';
import { AuthContext } from '../context/AuthContext';
import { PATHS } from '../constants/constants';
import './style/postFullPage.css';

const normalizeId = (id) => {
    if (id === null || id === undefined) return null;
    if (typeof id === 'number') return id;
    const numeric = Number(id);
    if (!Number.isNaN(numeric)) return numeric;
    const hex = parseInt(id, 16);
    return Number.isNaN(hex) ? null : hex;
};

const CommentItem = ({ comment, postOwnerId, onVoteUpdate, onRemoveComment, onEdit, onPostCommentAdded, onIncrementTopLevelReplies }) => {
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

    const commentOwnerComparableId = normalizeId(comment.ownerId);
    const isCommentOwner = commentOwnerComparableId !== null && user?.id === commentOwnerComparableId;
    const isPostOwner = postOwnerId ? normalizeId(postOwnerId) === user?.id : false;
    const canDelete = isCommentOwner || isPostOwner;

    const ownerProfileId = comment.ownerId || comment.userId || comment.owner?.id || '';
    const ownerUsername = comment.ownerName?.username || comment.username || comment.ownerUsername || comment.owner?.username;
    const ownerFirst = comment.ownerName?.firstName || comment.firstName || comment.owner?.firstName;
    const ownerLast = comment.ownerName?.lastName || comment.lastName || comment.owner?.lastName;
    const ownerFullName = comment.ownerFullName || comment.owner?.fullName || undefined;
    const [fetchedName, setFetchedName] = useState(null);
    useEffect(() => {
        let ignore = false;
        if (ownerProfileId) {
            fetchUserNameById(ownerProfileId).then(name => {
                if (!ignore && name) setFetchedName(name);
            });
        }
        return () => { ignore = true; };
    }, [ownerProfileId]);
    const fullName = (ownerFullName || `${ownerFirst || ''} ${ownerLast || ''}`).trim();
    let displayName = fetchedName || fullName || ownerUsername || (comment.email ? comment.email.split('@')[0] : null) || (commentOwnerComparableId !== null ? `User ${commentOwnerComparableId}` : 'User');
    if (isCommentOwner) {
        const selfFull = `${user?.firstName || ''} ${user?.lastName || ''}`.trim();
        if (selfFull) displayName = selfFull;
    }
    const avatarSrc = (isCommentOwner && user?.avatar) || comment.ownerAvatar || comment.avatar || comment.owner?.avatar;

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

    const [showReplyBox, setShowReplyBox] = useState(false);
    const [replyText, setReplyText] = useState('');
    const [submittingReply, setSubmittingReply] = useState(false);
    const [showReplies, setShowReplies] = useState(false);
    const [replies, setReplies] = useState(comment.replies || []);
    const [repliesLoading, setRepliesLoading] = useState(false);
    const [repliesPage, setRepliesPage] = useState(0);
    const [repliesHasMore, setRepliesHasMore] = useState(false);

    const REPLIES_PAGE_SIZE = 5;
    const loadReplies = async (page = 0) => {
        if (!comment.id) return;
        setRepliesLoading(true);
        const sortBy = 'score';
        try {
            // Assume getRepliesApi supports pagination: add page/size if needed
            const res = await getRepliesApi({ parentId: comment.id, sortBy, page, size: REPLIES_PAGE_SIZE });
            if (!res.success) throw new Error('Failed to fetch replies');
            const list = res.data || [];
            if (page === 0) {
                setReplies(list);
            } else {
                setReplies(prev => [...prev, ...list]);
            }
            setRepliesPage(page);
            setRepliesHasMore(list.length === REPLIES_PAGE_SIZE);
            onEdit && onEdit({ commentId: comment.id, numberOfReplies: (comment.numberOfReplies || 0) });
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
        if ((replies || []).length === 0) await loadReplies(0);
        setShowReplies(true);
    };

    const handleLoadMoreReplies = async () => {
        await loadReplies(repliesPage + 1);
    };

    const handleReplySubmit = async () => {
        if (!replyText.trim() || submittingReply) return;
        setSubmittingReply(true);
        const nowIso = new Date().toISOString();
        const ownerHex = user?.id ? user.id.toString(16).padStart(24, '0') : '0'.padStart(24, '0');
        const optimisticReply = {
            id: Math.random().toString(36).slice(2) + nowIso, // temp id
            ownerId: ownerHex,
            postId: comment.postId,
            parentId: comment.id,
            content: replyText.trim(),
            upvoteCount: 0,
            downvoteCount: 0,
            score: 0,
            depth: (comment.depth || 0) + 1,
            numberOfReplies: 0,
            createdAt: nowIso,
            ownerName: user ? {
                firstName: user.firstName,
                lastName: user.lastName,
                username: user.email || undefined
            } : undefined,
            ownerAvatar: user?.avatar,
            email: user?.email,
            isOptimistic: true
        };
        setReplies(prev => [optimisticReply, ...(prev || [])]);
        setReplyText('');
        setShowReplyBox(false);
        setShowReplies(true);
        // Simple cache for user names. Use forum API helper so we get axios + auth handling.
        onPostCommentAdded && onPostCommentAdded();
        onEdit && onEdit({ commentId: comment.id, numberOfReplies: (comment.numberOfReplies || 0) + 1 });
        if (typeof onIncrementTopLevelReplies === 'function') onIncrementTopLevelReplies(comment.id, 1);
        try {
            const res = await addCommentApi({ postId: comment.postId, parentId: comment.id, content: optimisticReply.content });
            if (res.success) {
                // Replace optimistic reply with real one (with real id)
                setReplies(prev => prev.map(r => r.isOptimistic ? {
                    ...r,
                    id: res.data,
                    isOptimistic: false
                } : r));
            } else {
                // Remove optimistic reply on failure
                setReplies(prev => prev.filter(r => !r.isOptimistic));
                console.error('Failed to post reply:', res.message);
            }
        } catch (e) {
            setReplies(prev => prev.filter(r => !r.isOptimistic));
            console.error('Error posting reply:', e);
        } finally {
            setSubmittingReply(false);
        }
    };

    const repliesCountRaw = (() => {
        // Prefer live loaded replies count when visible; otherwise fall back to server hint
        if (showReplies) return (replies || []).length;
        if (typeof comment.numberOfReplies === 'number') return comment.numberOfReplies;
        if (Array.isArray(comment.replies)) return comment.replies.length;
        return undefined;
    })();
    const repliesCount = (typeof repliesCountRaw === 'number') ? repliesCountRaw : undefined;
    const showRepliesButton = repliesCount > 0;

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
                    {ownerProfileId ? (
                        <Link
                            to={PATHS.USER.PROFILE(ownerProfileId)}
                            className="comment-avatar-link"
                            aria-label={`${displayName} profile`}
                        >
                            <div className="comment-avatar">
                                {avatarSrc ? <img src={avatarSrc} alt={`${displayName} avatar`} /> : <IoIosPerson />}
                            </div>
                        </Link>
                    ) : (
                        <div className="comment-avatar">
                            {avatarSrc ? <img src={avatarSrc} alt={`${displayName} avatar`} /> : <IoIosPerson />}
                        </div>
                    )}
                    {ownerProfileId ? (
                        <Link to={PATHS.USER.PROFILE(ownerProfileId)} className="comment-author-link">
                            <span className="comment-author">{displayName}</span>
                        </Link>
                    ) : (
                        <span className="comment-author">{displayName}</span>
                    )}
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
                    { showRepliesButton && (
                        <button className="view-replies-btn" onClick={handleViewReplies}>{showReplies ? 'Hide' : (typeof repliesCount === 'number' ? `View ${repliesCount} replies` : 'View replies')}</button>
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
                        {repliesLoading && <div className="loading-comments">Loading replies...</div>}
                        {replies.map(r => (
                            <CommentItem key={r.id} comment={r} postOwnerId={postOwnerId}
                                onVoteUpdate={onVoteUpdate}
                                onRemoveComment={(rid) => {
                                    onRemoveComment && onRemoveComment(rid);
                                    setReplies(prev => prev.filter(x => x.id !== rid));
                                }}
                                onEdit={({ commentId, content, numberOfReplies }) => setReplies(prev => prev.map(x => x.id === commentId ? { ...x, ...(content !== undefined ? { content } : {}), ...(numberOfReplies !== undefined ? { numberOfReplies } : {}) } : x))}
                                onPostCommentAdded={onPostCommentAdded}
                                onIncrementTopLevelReplies={onIncrementTopLevelReplies}
                            />
                        ))}
                        {repliesHasMore && !repliesLoading && (
                            <button className="load-more-replies-btn" onClick={handleLoadMoreReplies}>Load more replies</button>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

const computeTotalComments = (arr) => {
    const countRepliesRecursive = (items) => (items || []).reduce((sum, it) => sum + 1 + countRepliesRecursive(it.replies || []), 0);
    return (arr || []).reduce((sum, it) => sum + 1 + countRepliesRecursive(it.replies || []), 0);
};

const PostComments = ({ postId, postOwnerId, onCommentCountChange }) => {
    const [sort, setSort] = useState('best');
    const [commentText, setCommentText] = useState('');
    const [isSubmittingComment, setIsSubmittingComment] = useState(false);
    const [comments, setComments] = useState([]);
    const [commentsLoading, setCommentsLoading] = useState(false);

    const loadComments = useCallback(async () => {
        if (!postId) return;
        setCommentsLoading(true);
        try {
            const sortBy = sort === 'best' ? 'score' : 'new';
            const result = await getPostCommentsApi({
                postId,
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
                        return new Date(b.createdAt || 0) - new Date(a.createdAt || 0);
                    });
                } else if (sort === 'new') {
                    loaded = [...loaded].sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0));
                }
                const fetchRepliesTree = async (parentId) => {
                    const res = await getRepliesApi({ parentId, sortBy: 'score' });
                    if (!res.success) return [];
                    const list = res.data || [];
                    const nodes = await Promise.all(list.map(async (r) => {
                        r.replies = await fetchRepliesTree(r.id);
                        r.numberOfReplies = Math.max(r.numberOfReplies || 0, (r.replies || []).length);
                        return r;
                    }));
                    return nodes;
                };

                const loadedWithReplies = await Promise.all(loaded.map(async c => {
                    // Always fetch replies to ensure accurate counts and deep chains
                    const replies = await fetchRepliesTree(c.id);
                    return { ...c, replies, numberOfReplies: Math.max(c.numberOfReplies || 0, replies.length) };
                }));
                setComments(loadedWithReplies);
            }
        } catch (error) {
            console.error('Error loading comments:', error);
        } finally {
            setCommentsLoading(false);
        }
    }, [postId, sort]);

    useEffect(() => {
        loadComments();
    }, [loadComments]);

    useEffect(() => {
        const total = computeTotalComments(comments);
        if (typeof onCommentCountChange === 'function') onCommentCountChange(total);
        // Broadcast comment count update for global listeners (e.g., PostCard)
        if (postId && typeof total === 'number') {
            window.dispatchEvent(new CustomEvent('postCommentCountUpdated', {
                detail: { postId, commentCount: total }
            }));
        }
        // Intentionally exclude onCommentCountChange from deps to avoid infinite loops
        // when parent recreates the handler per render.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [comments]);

    const handleCommentSubmit = async () => {
        if (!commentText.trim() || isSubmittingComment || !postId) {
            return;
        }

        setIsSubmittingComment(true);
        try {
            const result = await addCommentApi({
                postId,
                parentId: null,
                content: commentText.trim()
            });

            if (result.success) {
                setCommentText('');
                await loadComments();
            } else {
                console.error('Failed to post comment:', result.message);
            }
        } catch (error) {
            console.error('Error posting comment:', error);
        } finally {
            setIsSubmittingComment(false);
        }
    };

    const incrementTopLevelRepliesFor = (targetId, delta = 1) => {
        setComments(prev => {
            if (!prev) return prev;
            const updated = prev.map(c => {
                if (c.id === targetId) {
                    return { ...c, numberOfReplies: (c.numberOfReplies || 0) + delta };
                }
                const findInReplies = (repliesArr, tid) => {
                    for (const r of repliesArr || []) {
                        if (r.id === tid) return true;
                        if (r.replies && findInReplies(r.replies, tid)) return true;
                    }
                    return false;
                };
                const found = findInReplies(c.replies || [], targetId);
                if (found) {
                    return { ...c, numberOfReplies: (c.numberOfReplies || 0) + delta };
                }
                return c;
            });
            return updated;
        });
    };

    return (
        <div>
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
                                postOwnerId={postOwnerId}
                                onPostCommentAdded={() => loadComments()}
                                onIncrementTopLevelReplies={(id, delta) => incrementTopLevelRepliesFor(id, delta)}
                                onVoteUpdate={(payload) => {
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
                                                    return { ...c, replies: newReplies, numberOfReplies: Math.max((c.numberOfReplies || 1) - 1, 0) };
                                                }
                                                return c;
                                            });
                                            return mapped;
                                        }
                                        return filtered;
                                    });
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

export default PostComments;
