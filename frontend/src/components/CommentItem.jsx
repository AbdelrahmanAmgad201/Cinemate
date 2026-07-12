import { useState, useEffect, useRef, useContext } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { User, MoreVertical } from 'lucide-react';
import { addCommentApi, deleteCommentApi, getRepliesApi } from '../api/comment-api';
import { MAX_LENGTHS, PATHS } from '../constants/constants';
import { normalizeId, computeTotalComments } from '../utils/commentTree.jsx';
import { useUserNameCache } from '../hooks/useUserNameCache.jsx';
import VoteWidget from './VoteWidget';
import ConfirmDialog from './ui/ConfirmDialog.jsx';
import { ToastContext } from '../context/ToastContext';
import { AuthContext } from '../context/AuthContext';
import './style/postThread.css';

const INLINE_MAX_DEPTH = 2;

const ThreadLinkButton = ({ comment, repliesCount, post }) => {
    const navigate = useNavigate();
    const handleOpenThread = () => {
        const url = `${PATHS.POST.THREAD(comment.id)}?postId=${encodeURIComponent(comment.postId || '')}`;
        navigate(url, { state: { comment, postId: comment.postId, post } });
    };
    return (
        <button className="view-thread-btn" onClick={handleOpenThread}>{`Open thread (${(typeof repliesCount === 'number' ? repliesCount : (comment.numberOfReplies || 'some'))} replies)`}</button>
    );
};

export const CommentItem = ({ comment, post, postOwnerId, onVoteUpdate, onRemoveComment, onEdit, onPostCommentAdded, onIncrementTopLevelReplies, inlineMaxDepth = INLINE_MAX_DEPTH, maxInlineReplies = Infinity, isModal = false, hideViewReplies = false, hideReplyButton = false, fetchRepliesTreeOnOpen = false, preferInlineReplies = false }) => {
    const { user } = useContext(AuthContext);
    const [menuOpen, setMenuOpen] = useState(false);
    const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);
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

    // Toast helper from context
    const { showToast } = useContext(ToastContext);

    const ownerProfileId = comment.ownerId || comment.userId || comment.owner?.id || '';
    const ownerUsername = comment.ownerName?.username || comment.username || comment.ownerUsername || comment.owner?.username;
    const ownerFirst = comment.ownerName?.firstName || comment.firstName || comment.owner?.firstName;
    const ownerLast = comment.ownerName?.lastName || comment.lastName || comment.owner?.lastName;
    const ownerFullName = comment.ownerFullName || comment.owner?.fullName || undefined;
    const fetchedName = useUserNameCache(ownerProfileId);
    const fullName = (ownerFullName || `${ownerFirst || ''} ${ownerLast || ''}`).trim();
    let displayName = fetchedName || fullName || ownerUsername || (comment.email ? comment.email.split('@')[0] : null) || (commentOwnerComparableId !== null ? `User ${commentOwnerComparableId}` : 'User');
    if (isCommentOwner) {
        const selfFull = `${user?.firstName || ''} ${user?.lastName || ''}`.trim();
        if (selfFull) displayName = selfFull;
    }
    const avatarSrc = (isCommentOwner && user?.avatar) || comment.ownerAvatar || comment.avatar || comment.owner?.avatar;

    const handleDeleteClick = async () => {
        setConfirmDeleteOpen(false);

        try {
            const res = await deleteCommentApi({ commentId: comment.id });
            if (res.success) {
                onRemoveComment && onRemoveComment(comment.id);
                try { showToast('', 'Comment deleted', 'success'); } catch { /* ignore */ }
            } else {
                console.error('Failed to delete comment:', res.message);
                try { showToast('Error', 'Failed to delete comment: ' + (res.message || 'Unknown error'), 'error'); } catch { /* ignore */ }
            }
        } catch (e) {
            console.error('Error deleting comment:', e);
            try { showToast('Error', 'Error deleting comment', 'error'); } catch { /* ignore */ }
        }
    };

    const [showReplyBox, setShowReplyBox] = useState(false);
    const [replyText, setReplyText] = useState('');
    const [submittingReply, setSubmittingReply] = useState(false);
    const [showReplies, setShowReplies] = useState(false);
    const [replies, setReplies] = useState(comment.replies || []);
    const [repliesTotalCount, setRepliesTotalCount] = useState(undefined);
    const [repliesLoading, setRepliesLoading] = useState(false);
    const [repliesPage, setRepliesPage] = useState(0);
    const [repliesHasMore, setRepliesHasMore] = useState(false);

    const REPLIES_PAGE_SIZE = 5;
    const COUNT_PAGE_SIZE = 20;
    const loadReplies = async (page = 0) => {
        if (!comment.id) return;
        setRepliesLoading(true);
        const sortBy = 'score';
        try {
            const fetchRepliesTree = async (parentId) => {
                const r = await getRepliesApi({ parentId, sortBy });
                if (!r.success) return [];
                const list = r.data || [];
                const nodes = await Promise.all(list.map(async (it) => {
                    it.replies = await fetchRepliesTree(it.id);
                    it.numberOfReplies = Math.max(it.numberOfReplies || 0, (it.replies || []).length);
                    return it;
                }));
                return nodes;
            };

            if (fetchRepliesTreeOnOpen && page === 0) {
                const fullList = await fetchRepliesTree(comment.id);
                setReplies(fullList);
                setRepliesPage(0);
                setRepliesHasMore(false);
                onEdit && onEdit({ commentId: comment.id, numberOfReplies: (comment.numberOfReplies || 0) });

                const computeFromLoaded = (items) => {
                    const countRecursive = (arr) => (arr || []).reduce((s, it) => s + 1 + countRecursive(it.replies || []), 0);
                    return countRecursive(items);
                };
                setRepliesTotalCount(computeFromLoaded(fullList));
                return;
            }

            const res = await getRepliesApi({ parentId: comment.id, sortBy, page, size: REPLIES_PAGE_SIZE });
            if (!res.success) throw new Error('Failed to fetch replies');
            const list = res.data || [];

            if ((list || []).length === 0 && page === 0) {
                const deeper = await fetchRepliesTree(comment.id);
                if (deeper && deeper.length > 0) {
                    setReplies(deeper);
                    setRepliesPage(0);
                    setRepliesHasMore(false);
                    setRepliesTotalCount((deeper || []).length);
                    onEdit && onEdit({ commentId: comment.id, numberOfReplies: (comment.numberOfReplies || 0) });
                    return;
                }
            }

            if (page === 0) {
                setReplies(list);
            } else {
                setReplies(prev => [...prev, ...list]);
            }
            setRepliesPage(page);
            setRepliesHasMore(list.length === REPLIES_PAGE_SIZE);
            onEdit && onEdit({ commentId: comment.id, numberOfReplies: (comment.numberOfReplies || 0) });

            const computeFromLoaded = (items) => {
                const countRecursive = (arr) => (arr || []).reduce((s, it) => s + 1 + countRecursive(it.replies || []), 0);
                return countRecursive(items);
            };
            const loadedTotal = computeFromLoaded(list);
            if (!repliesHasMore && (list || []).length > 0) {
                (async () => {
                    let acc = 0;
                    const stack = [...list];
                    while (stack.length) {
                        const node = stack.pop();
                        acc += 1;
                        let p = 0;
                        while (true) {
                            const r = await getRepliesApi({ parentId: node.id, sortBy: 'score', page: p, size: COUNT_PAGE_SIZE });
                            if (!r.success) break;
                            const items = r.data || [];
                            for (const child of items) stack.push(child);
                            if (items.length < COUNT_PAGE_SIZE) break;
                            p++;
                        }
                    }
                    const descendants = Math.max(0, acc - (list || []).length);
                    setRepliesTotalCount((list || []).length + descendants);
                })();
            } else {
                setRepliesTotalCount(loadedTotal + (comment.numberOfReplies ? 0 : 0));
            }
        } catch (e) {
            console.error('Error loading replies:', e);
        } finally {
            setRepliesLoading(false);
        }
    };

    useEffect(() => {
        let mounted = true;
        if (Array.isArray(comment.replies) && comment.replies.length > 0) {
            const countRecursive = (arr) => (arr || []).reduce((s, it) => s + 1 + countRecursive(it.replies || []), 0);
            if (mounted) setRepliesTotalCount(countRecursive(comment.replies));
            return () => { mounted = false; };
        }

        if ((comment.numberOfReplies || 0) > 0 && repliesTotalCount === undefined) {
            (async () => {
                let total = 0;
                const stack = [comment.id];
                while (stack.length) {
                    const pid = stack.pop();
                    let page = 0;
                    while (true) {
                        const res = await getRepliesApi({ parentId: pid, sortBy: 'score', page, size: COUNT_PAGE_SIZE });
                        if (!res.success) break;
                        const items = res.data || [];
                        total += items.length;
                        for (const it of items) stack.push(it.id);
                        if (items.length < COUNT_PAGE_SIZE) break;
                        page++;
                    }
                }
                if (mounted) setRepliesTotalCount(total);
            })();
        }
        return () => { mounted = false; };
    }, [comment.id, comment.replies, comment.numberOfReplies]);

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
        if (comment.isDeleted) {
            setShowReplyBox(false);
            try { showToast('Error', 'Cannot reply to a deleted comment.', 'error'); } catch { /* ignore */ }
            return;
        }
        setSubmittingReply(true);
        const nowIso = new Date().toISOString();
        const optimisticReply = {
            id: Math.random().toString(36).slice(2) + nowIso, // temp id
            ownerId: user?.id ?? null,
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
        // optimistic total update for UI
        setRepliesTotalCount(prev => (typeof prev === 'number' ? prev + 1 : (comment.numberOfReplies || 0) + 1));
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
                setRepliesTotalCount(prev => (typeof prev === 'number' ? prev : (comment.numberOfReplies || 0)) );
            } else {
                // Remove optimistic reply on failure
                setReplies(prev => prev.filter(r => !r.isOptimistic));
                setRepliesTotalCount(prev => (typeof prev === 'number' ? Math.max(0, prev - 1) : undefined));
                try { showToast('Error', 'Failed to post reply: ' + (res.message || 'Unknown error'), 'error'); } catch { /* ignore */ }
                console.error('Failed to post reply:', res.message);
            }
        } catch (e) {
            setReplies(prev => prev.filter(r => !r.isOptimistic));
            try { showToast('Error', 'Error posting reply'); } catch { /* ignore */ }
            console.error('Error posting reply:', e);
        } finally {
            setSubmittingReply(false);
        }
    };

    const repliesCountRaw = (() => {
        if (showReplies && typeof repliesTotalCount === 'number') return repliesTotalCount;
        if (showReplies) return computeTotalComments([comment]) - 1; // exclude the parent itself
        if (typeof repliesTotalCount === 'number') return repliesTotalCount;
        if (typeof comment.numberOfReplies === 'number') return comment.numberOfReplies;
        if (Array.isArray(comment.replies)) return comment.replies.length;
        return undefined;
    })();
    const repliesCount = (typeof repliesCountRaw === 'number') ? repliesCountRaw : undefined;
    const showRepliesButton = repliesCount > 0;

    return (
        <div id={`comment-${comment.id}`} className="comment-item">
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
                                {avatarSrc ? <img src={avatarSrc} alt={`${displayName} avatar`} /> : <User size={16} />}
                            </div>
                        </Link>
                    ) : (
                        <div className="comment-avatar">
                            {avatarSrc ? <img src={avatarSrc} alt={`${displayName} avatar`} /> : <User size={16} />}
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
                            <MoreVertical size={16} onClick={() => setMenuOpen(prev => !prev)} />
                            {menuOpen && (
                                <div className="options-menu">
                                    <ul>
                                        {canDelete && (
                                            <li onClick={() => { setMenuOpen(false); setConfirmDeleteOpen(true); }}>Delete</li>
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
                <ConfirmDialog
                    open={confirmDeleteOpen}
                    onClose={() => setConfirmDeleteOpen(false)}
                    onConfirm={handleDeleteClick}
                    title="Delete comment?"
                    message="Are you sure you want to delete this comment? This can't be undone."
                    confirmLabel="Delete"
                    danger
                />
                <div className="comment-actions">
                    { !hideReplyButton && !comment.isDeleted && (
                        <button className="reply-btn" onClick={() => setShowReplyBox(prev => !prev)}>Reply</button>
                    ) }
                    { comment.isDeleted && (
                        <span style={{ color: 'rgba(168,168,168,0.9)', fontSize: 13 }}>This comment has been deleted</span>
                    ) }
                    { showRepliesButton && !hideViewReplies && (
                        <button className="view-replies-btn" onClick={handleViewReplies}>{showReplies ? 'Hide' : (typeof repliesCount === 'number' ? `View ${repliesCount} replies` : 'View replies')}</button>
                    ) }
                </div>
                {showReplyBox && (
                    <div className="reply-box">
                        <textarea
                            value={replyText}
                            onChange={(e) => setReplyText(e.target.value)}
                            placeholder="Write a reply..."
                            maxLength={MAX_LENGTHS.TEXTAREA}
                        />
                        <div className="char-count" aria-hidden="true">{replyText.length}/{MAX_LENGTHS.TEXTAREA}</div>
                        <div className="reply-actions">
                            <button className="comment-btn" onClick={handleReplySubmit} disabled={!replyText.trim() || submittingReply}>{submittingReply ? 'Replying...' : 'Reply'}</button>
                            <button className="comment-btn" onClick={() => setShowReplyBox(false)}>Cancel</button>
                        </div>
                    </div>
                )}
                {showReplies && (
                    <div className="comment-replies">
                        {repliesLoading && <div className="loading-comments">Loading replies...</div>}

                        {/* If this comment is already deep, don't render further inline replies (to avoid extreme indentation)
                            instead show a compact button to open the full thread as a dedicated page */}
                        {(comment.depth || 0) >= inlineMaxDepth && !isModal ? (
                            (preferInlineReplies ? (
                                replies.map(r => (
                                    <CommentItem key={r.id} comment={r} post={post} postOwnerId={postOwnerId}
                                        onVoteUpdate={onVoteUpdate}
                                        onRemoveComment={(rid) => {
                                            onRemoveComment && onRemoveComment(rid);
                                            setReplies(prev => prev.filter(x => x.id !== rid));
                                            setRepliesTotalCount(prev => (typeof prev === 'number' ? Math.max(0, prev - 1) : undefined));
                                        }}
                                        onEdit={({ commentId, content, numberOfReplies }) => setReplies(prev => prev.map(x => x.id === commentId ? { ...x, ...(content !== undefined ? { content } : {}), ...(numberOfReplies !== undefined ? { numberOfReplies } : {}) } : x)) }
                                        onPostCommentAdded={onPostCommentAdded}
                                        onIncrementTopLevelReplies={onIncrementTopLevelReplies}
                                        inlineMaxDepth={inlineMaxDepth}
                                        maxInlineReplies={maxInlineReplies}
                                        fetchRepliesTreeOnOpen={fetchRepliesTreeOnOpen}
                                        preferInlineReplies={preferInlineReplies}
                                    />
                                ))
                            ) : (
                                <div className="deep-thread-placeholder">
                                    <ThreadLinkButton comment={comment} repliesCount={repliesCount} post={post} />
                                </div>
                            ))
                        ) : (
                            replies.slice(0, maxInlineReplies).map(r => (
                                <CommentItem key={r.id} comment={r} postOwnerId={postOwnerId}
                                    onVoteUpdate={onVoteUpdate}
                                    onRemoveComment={(rid) => {
                                        onRemoveComment && onRemoveComment(rid);
                                        setReplies(prev => prev.filter(x => x.id !== rid));
                                        setRepliesTotalCount(prev => (typeof prev === 'number' ? Math.max(0, prev - 1) : undefined));
                                    }}
                                    onEdit={({ commentId, content, numberOfReplies }) => setReplies(prev => prev.map(x => x.id === commentId ? { ...x, ...(content !== undefined ? { content } : {}), ...(numberOfReplies !== undefined ? { numberOfReplies } : {}) } : x))}
                                    onPostCommentAdded={onPostCommentAdded}
                                    onIncrementTopLevelReplies={onIncrementTopLevelReplies}
                                    inlineMaxDepth={inlineMaxDepth}
                                    maxInlineReplies={maxInlineReplies}
                                    fetchRepliesTreeOnOpen={fetchRepliesTreeOnOpen}
                                    preferInlineReplies={preferInlineReplies}
                                />
                            ))
                        )}

                        {showReplies && replies && replies.length > maxInlineReplies && maxInlineReplies !== Infinity && (
                            <div className="deep-thread-placeholder">
                                <ThreadLinkButton comment={comment} repliesCount={repliesCount} post={post} />
                            </div>
                        )}

                        {repliesHasMore && !repliesLoading && maxInlineReplies === Infinity && (
                            <button className="load-more-replies-btn" onClick={handleLoadMoreReplies}>Load more replies</button>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default CommentItem;
