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
import { Link, useNavigate } from 'react-router-dom';
import { IoIosPerson } from 'react-icons/io';
import { BsThreeDots } from 'react-icons/bs';
import { MdKeyboardArrowDown } from 'react-icons/md';
import { addCommentApi, getPostCommentsApi, deleteCommentApi, getRepliesApi } from '../api/comment-api';
import { MAX_LENGTHS } from '../constants/constants';
import VoteWidget from './VoteWidget';
import { ToastContext } from '../context/ToastContext';
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

const INLINE_MAX_DEPTH = 2;

const ThreadLinkButton = ({ comment, repliesCount, post }) => {
    const navigate = useNavigate();
    const handleOpenThread = () => {
        const url = `${PATHS.POST.THREAD(comment.id)}?postId=${encodeURIComponent(comment.postId || '')}`;
        // If we have a locally cached vote update for this comment, merge it into the navigation state
        // so the thread page shows the latest client-side counts even if the parent state hasn't re-rendered yet.
        let navComment = comment;
        try {
            const cached = JSON.parse(sessionStorage.getItem(`CINEMATE_LAST_COMMENT_${comment.id}`) || 'null');
            if (cached) navComment = { ...comment, ...cached };
        } catch (e) { /* ignore storage errors */ }
        navigate(url, { state: { comment: navComment, postId: comment.postId, post } });
    };
    return (
        <button className="view-thread-btn" onClick={handleOpenThread}>{`Open thread (${(typeof repliesCount === 'number' ? repliesCount : (comment.numberOfReplies || 'some'))} replies)`}</button>
    );
};

const CommentItem = ({ comment, post, postOwnerId, onVoteUpdate, onRemoveComment, onEdit, onPostCommentAdded, onIncrementTopLevelReplies, inlineMaxDepth = INLINE_MAX_DEPTH, maxInlineReplies = Infinity, isModal = false, hideViewReplies = false, hideReplyButton = false, fetchRepliesTreeOnOpen = false, preferInlineReplies = false }) => {
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

    const { showToast } = useContext(ToastContext);

    const handleReplySubmit = async () => {
        if (!replyText.trim() || submittingReply) return;
        if (comment.isDeleted) {
            setShowReplyBox(false);
            try { showToast('Error', 'Cannot reply to a deleted comment.', 'error'); } catch (e) {}
            return;
        }
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
        // optimistic total update for UI
        setRepliesTotalCount(prev => (typeof prev === 'number' ? prev + 1 : (comment.numberOfReplies || 0) + 1));
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
                setRepliesTotalCount(prev => (typeof prev === 'number' ? prev : (comment.numberOfReplies || 0)) );
            } else {
                // Remove optimistic reply on failure
                setReplies(prev => prev.filter(r => !r.isOptimistic));
                setRepliesTotalCount(prev => (typeof prev === 'number' ? Math.max(0, prev - 1) : undefined));
                try { showToast('Error', 'Failed to post reply: ' + (res.message || 'Unknown error'), 'error'); } catch (e) {}
                console.error('Failed to post reply:', res.message);
            }
        } catch (e) {
            setReplies(prev => prev.filter(r => !r.isOptimistic));
            try { showToast('Error', 'Error posting reply'); } catch (err) {}
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

const computeTotalComments = (arr) => {
    const countRepliesRecursive = (items) => (items || []).reduce((sum, it) => sum + 1 + countRepliesRecursive(it.replies || []), 0);
    return (arr || []).reduce((sum, it) => sum + 1 + countRepliesRecursive(it.replies || []), 0);
};

const PostComments = ({ postId, post, postOwnerId, onCommentCountChange }) => {
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
                const mergedWithCache = loadedWithReplies.map(c => {
                    try {
                        const cached = JSON.parse(sessionStorage.getItem(`CINEMATE_LAST_COMMENT_${c.id}`) || 'null');
                        if (cached) return { ...c, ...cached };
                    } catch (e) { /* ignore */ }
                    return c;
                });
                setComments(mergedWithCache);
                console.debug('[Comments] Loaded comments for', postId, { topCount: loaded.length, loadedWithReplies: mergedWithCache.length });
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
            try {
                sessionStorage.setItem(`CINEMATE_LAST_COMMENT_COUNT_${postId}`, JSON.stringify({ count: total, ts: Date.now() }));
            } catch (e) {
                // ignore storage errors
            }
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
            const payload = { postId, parentId: null, content: commentText.trim() };
            console.debug('[Comments] Posting comment', { postId, parentId: null, length: payload.content.length });
            const result = await addCommentApi(payload);
            console.debug('[Comments] Post comment result', { postId, success: result?.success, message: result?.message });

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
                    maxLength={MAX_LENGTHS.TEXTAREA}
                />
                <div className="char-count" aria-hidden="true">{commentText.length}/{MAX_LENGTHS.TEXTAREA}</div>
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
                                post={post}
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

                                        try {
                                            const target = updated.find(x => x.id === payload.targetId);
                                            if (target) {
                                                sessionStorage.setItem(`CINEMATE_LAST_COMMENT_${target.id}`, JSON.stringify({ upvoteCount: target.upvoteCount, downvoteCount: target.downvoteCount, ts: Date.now() }));
                                            }
                                        } catch (e) { /* ignore storage errors */ }

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
export { CommentItem };
