import React, { useState, useEffect, useContext } from 'react';
import { useLocation, useParams, useNavigate, Link } from 'react-router-dom';
import { CommentItem } from '../../components/PostComments';
import { getRepliesApi, getPostCommentsApi, addCommentApi } from '../../api/comment-api';
import { getPostApi } from '../../api/post-api';
import { PATHS, MAX_LENGTHS } from '../../constants/constants';
import { MdKeyboardArrowDown } from 'react-icons/md';
import '../../components/style/postFullPage.css';
import '../../components/style/postCard.css';
import { ToastContext } from '../../context/ToastContext';

const ReplyThreadPage = () => {
    const { commentId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();

    const initialComment = location.state?.comment || null;
    const postIdFromState = location.state?.postId || null;
    const searchParams = new URLSearchParams(location.search || '');
    const postIdFromQuery = searchParams.get('postId') || null;

    // If the thread was opened with the full post in navigation state cache it
    // so navigating back can reuse it even if the state is lost
    useEffect(() => {
        const suppliedPost = location.state?.post;
        if (suppliedPost && suppliedPost.id) {
            try {
                sessionStorage.setItem(`CINEMATE_LAST_POST_${suppliedPost.id}`, JSON.stringify(suppliedPost));
            } catch (e) {
                // ignore storage errors
            }
        }
    }, [location.state]);

    const [comment, setComment] = useState(initialComment);
    const [replies, setReplies] = useState([]);
    const [loading, setLoading] = useState(false);
    const [sort, setSort] = useState('best');
    const [replyText, setReplyText] = useState('');
    const [isSubmittingReply, setIsSubmittingReply] = useState(false);

    const REPLIES_PAGE_SIZE = 3;

    const { showToast } = useContext(ToastContext);

    useEffect(() => {
        try {
            window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
        } catch (e) {
            // ignore in environments without window
        }
    }, [commentId]);

    const filterOutSelf = (list, parentId) => (list || []).filter(r => String(r.id) !== String(parentId));

    const findCommentInTree = (nodes, targetId) => {
        for (const n of nodes || []) {
            if (String(n.id) === String(targetId)) return n;
            if (n.replies && n.replies.length) {
                const found = findCommentInTree(n.replies, targetId);
                if (found) return found;
            }
        }
        return null;
    };

    const enrichRepliesWithChildrenFlag = async (list) => {
        if (!list || !list.length) return list;
        try {
            const enriched = await Promise.all(list.map(async (it) => {
                if (typeof it.numberOfReplies === 'number' && it.numberOfReplies > 0) return it;
                try {
                    const res = await getRepliesApi({ parentId: it.id, page: 0, size: 1 });
                    if (res.success && res.data && res.data.length > 0) {
                        return { ...it, numberOfReplies: (it.numberOfReplies || 0) + res.data.length };
                    }
                } catch (e) {
                    // ignore per-item errors
                }
                return it;
            }));
            return enriched;
        } catch (e) {
            console.error('Error enriching replies with children flag', e);
            return list;
        }
    };

    useEffect(() => {
        setComment(null);
        setReplies([]);
        setLoading(true);

        const pid = postIdFromState || postIdFromQuery;
        if (!commentId || !pid) {
            setLoading(false);
            return;
        }

        if (initialComment && String(initialComment.id) === String(commentId)) {
            (async () => {
                try {
                    // If we have a cached local vote update for this comment (from a recent vote), merge it so refreshes
                    // show the latest client-side counts until the server reflects them.
                    let merged = initialComment;
                    try {
                        const cached = JSON.parse(sessionStorage.getItem(`CINEMATE_LAST_COMMENT_${initialComment.id}`) || 'null');
                        if (cached) merged = { ...initialComment, ...cached };
                    } catch (e) { /* ignore storage errors */ }

                    setComment(merged);
                    const sortBy = sort === 'best' ? 'score' : 'new';
                    const r = await getRepliesApi({ parentId: initialComment.id, sortBy, page: 0, size: REPLIES_PAGE_SIZE });
                    if (r.success) setReplies(await enrichRepliesWithChildrenFlag(filterOutSelf(r.data || [], initialComment.id)));
                } catch (e) {
                    console.error('Failed to load replies for thread page', e);
                } finally {
                    setLoading(false);
                }
            })();
            return;
        }

        (async () => {
            try {
                const res = await getPostCommentsApi({ postId: pid, page: 0, size: 50, sortBy: 'score' });
                if (res.success && res.data?.content) {
                    const top = res.data.content;
                    const enrich = async (node) => {
                        const rr = await getRepliesApi({ parentId: node.id, sortBy: 'score' });
                        node.replies = (rr.success && rr.data) ? rr.data : [];
                        for (const child of node.replies) {
                            const rc = await getRepliesApi({ parentId: child.id, sortBy: 'score' });
                            child.replies = (rc.success && rc.data) ? rc.data : [];
                        }
                        return node;
                    };
                    const enriched = await Promise.all(top.map(enrich));
                    let found = findCommentInTree(enriched, commentId);
                    if (found) {
                        try {
                            const cached = JSON.parse(sessionStorage.getItem(`CINEMATE_LAST_COMMENT_${found.id}`) || 'null');
                            if (cached) found = { ...found, ...cached };
                        } catch (e) { /* ignore storage errors */ }

                        setComment(found);
                        const sortBy = sort === 'best' ? 'score' : 'new';
                        const r = await getRepliesApi({ parentId: found.id, sortBy, page: 0, size: REPLIES_PAGE_SIZE });
                        if (r.success) setReplies(await enrichRepliesWithChildrenFlag(filterOutSelf(r.data || [], found.id)));
                    }
                }
            } catch (e) {
                console.error('Error finding comment for thread page', e);
            } finally {
                setLoading(false);
            }
        })();
    }, [commentId, initialComment, postIdFromState, postIdFromQuery, sort]);

    const handleReplySubmit = async () => {
        if (!replyText.trim() || isSubmittingReply || !comment) return;
        if (comment.isDeleted) {
            showToast("", "Cannot reply to a deleted comment.", "error");
            return;
        }
        setIsSubmittingReply(true);
        try {
            const res = await addCommentApi({ postId: comment.postId, parentId: comment.id, content: replyText.trim() });
            if (res.success) {
                setReplyText('');
                setLoading(true);
                const sortBy = sort === 'best' ? 'score' : 'new';
                const r2 = await getRepliesApi({ parentId: comment.id, sortBy, page: 0, size: REPLIES_PAGE_SIZE });
                if (r2.success) setReplies(await enrichRepliesWithChildrenFlag(filterOutSelf(r2.data || [], comment.id)));
            } else {
                console.error('Failed to post reply:', res.message);
                showToast("", "Failed to post reply: " + (res.message || 'Unknown error'), "error");
            }
        } catch (e) {
            console.error('Error posting reply:', e);
            showToast("", "Error posting reply", "error");
        } finally {
            setIsSubmittingReply(false);
            setLoading(false);
        }
    };

    // Update the main comment counts when a user votes on it. Also persist a small local cache so
    // a browser refresh will show the latest client-side counts until the backend catches up.
    const handleMainVoteUpdate = (payload) => {
        if (!payload || !comment || payload.targetId !== comment.id) return;
        const prev = payload.previousVote || 0;
        const next = payload.newVote || 0;
        const prevUp = prev === 1 ? 1 : 0;
        const prevDown = prev === -1 ? 1 : 0;
        const newUp = next === 1 ? 1 : 0;
        const newDown = next === -1 ? 1 : 0;
        const upDelta = newUp - prevUp;
        const downDelta = newDown - prevDown;
        setComment(prevC => {
            if (!prevC) return prevC;
            const updated = { ...prevC, upvoteCount: (prevC.upvoteCount || 0) + upDelta, downvoteCount: (prevC.downvoteCount || 0) + downDelta };
            try { sessionStorage.setItem(`CINEMATE_LAST_COMMENT_${updated.id}`, JSON.stringify({ upvoteCount: updated.upvoteCount, downvoteCount: updated.downvoteCount, ts: Date.now() })); } catch (e) { /* ignore */ }
            return updated;
        });
    };

    if (!comment) {
        return (
            <div className="post-page" style={{ padding: 24 }}>
                <h2>Thread</h2>
                <p>Comment data not available. Try opening this thread from the post it belongs to.
                    {postIdFromQuery ? (
                        <span> You can also <Link to={PATHS.POST.FULLPAGE(postIdFromQuery)} state={{ post: { id: postIdFromQuery, postId: postIdFromQuery }, commentId: commentId }}>open the post</Link> and find the comment there.</span>
                    ) : null}
                </p>

            </div>
        );
    }

    return (
        <div className="post-page thread-page">
            <div className="post-main-area" style={{ padding: 24 }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <h2>Thread</h2>
                    <button
                        className="back-to-post-btn btn-primary"
                        aria-label="Back to post"
                        onClick={async () => {
                            try {
                                const res = await getPostApi({ postId: comment.postId });
                                if (res.success) {
                                    navigate(PATHS.POST.FULLPAGE(comment.postId), { state: { post: res.data, commentId: comment.id } });
                                } else {
                                    navigate(PATHS.POST.FULLPAGE(comment.postId), { state: { post: { id: comment.postId, postId: comment.postId }, commentId: comment.id } });
                                }
                            } catch (e) {
                                navigate(PATHS.POST.FULLPAGE(comment.postId), { state: { post: { id: comment.postId, postId: comment.postId }, commentId: comment.id } });
                            }
                        }}
                    >
                        Back to post
                    </button>
                </div>

                <div style={{ marginTop: 12 }}>
                    <div className="thread-main-comment">
                        <CommentItem
                            comment={comment}
                            postOwnerId={comment.ownerId}
                            hideViewReplies={true}
                            hideReplyButton={true}
                            onVoteUpdate={handleMainVoteUpdate}
                            onPostCommentAdded={async () => {
                                setLoading(true);
                                const res = await getRepliesApi({ parentId: comment.id, sortBy: sort === 'best' ? 'score' : 'new', page: 0, size: REPLIES_PAGE_SIZE });
                                if (res.success) setReplies(await enrichRepliesWithChildrenFlag(res.data || []));
                                setLoading(false);
                            }}
                            onRemoveComment={async (id) => {
                                try {
                                    const res = await getPostApi({ postId: comment.postId });
                                    if (res.success) {
                                        navigate(PATHS.POST.FULLPAGE(comment.postId), { state: { post: res.data } });
                                    } else {
                                        navigate(PATHS.POST.FULLPAGE(comment.postId), { state: { post: { id: comment.postId, postId: comment.postId } } });
                                    }
                                } catch (e) {
                                    navigate(PATHS.POST.FULLPAGE(comment.postId), { state: { post: { id: comment.postId, postId: comment.postId } } });
                                }
                                showToast("", "This comment has been deleted. Returning to the post.", "info");
                            }}
                            inlineMaxDepth={2}
                            maxInlineReplies={3}
                        />
                    </div>
                </div>

                <div className="comment-input" style={{ marginTop: 18 }}>
                    <textarea
                        value={replyText}
                        onChange={(e) => setReplyText(e.target.value)}
                        placeholder="Write a reply"
                        maxLength={MAX_LENGTHS.TEXTAREA}
                        disabled={isSubmittingReply}
                    />
                    <div className="char-count" aria-hidden="true">{replyText.length}/{MAX_LENGTHS.TEXTAREA}</div>
                    <button className="comment-btn" onClick={handleReplySubmit} disabled={!replyText.trim() || isSubmittingReply}>{isSubmittingReply ? 'Posting...' : 'Reply'}</button>
                </div>

                <div className="comments-section" style={{ marginTop: 24 }}>
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

                    <h3>Replies</h3>
                    {loading ? (
                        <p>Loading replies...</p>
                    ) : (
                        <div>
                            {replies.length === 0 ? <p>No replies yet.</p> : (
                                replies.map(r => (
                                    <CommentItem key={r.id} comment={r} postOwnerId={comment.ownerId} inlineMaxDepth={2} maxInlineReplies={3} />
                                ))
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default ReplyThreadPage;
