import { useState, useEffect, useCallback } from 'react';
import { ChevronDown } from 'lucide-react';
import { addCommentApi, getPostCommentsApi, getRepliesApi } from '../api/comment-api';
import { MAX_LENGTHS } from '../constants/constants';
import { computeTotalComments } from '../utils/commentTree.jsx';
import { useCommentVoting, readCachedCommentVote } from '../hooks/useCommentVoting.jsx';
import { CommentItem } from './CommentItem.jsx';
import './style/postThread.css';

const PostComments = ({ postId, post, postOwnerId, onCommentCountChange }) => {
    const [sort, setSort] = useState('best');
    const [commentText, setCommentText] = useState('');
    const [isSubmittingComment, setIsSubmittingComment] = useState(false);
    const [comments, setComments] = useState([]);
    const [commentsLoading, setCommentsLoading] = useState(false);

    const handleCommentVote = useCommentVoting(setComments);

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
                    const cached = readCachedCommentVote(c.id);
                    return cached ? { ...c, ...cached } : c;
                });
                setComments(mergedWithCache);
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
            } catch {
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
            const result = await addCommentApi(payload);

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
                        <ChevronDown size={16} className="select-arrow" />
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
                                    handleCommentVote(payload);
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
