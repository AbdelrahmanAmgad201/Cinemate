import React, {useState, useEffect, useRef, useContext} from 'react';
import { useSearchParams } from 'react-router-dom';
import PostCard from '../../components/PostCard.jsx';
import LoadingFallback from '../../components/LoadingFallback.jsx';
import {getExploreFeedPostsApi, getMainFeedPostsApi} from '../../api/posts-api.jsx';
import mockPosts from '../../data/mock-posts.jsx';
import "./style/HomePage.css";
import {ToastContext} from "../../context/ToastContext.jsx";

const HomePage = () => {

    const [posts, setPosts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [size] = useState(10);
    const [hasMore, setHasMore] = useState(true);
    const [loadingMore, setLoadingMore] = useState(false);
    const sentinelRef = useRef(null);

    const { showToast } = useContext(ToastContext);

    const [searchParams] = useSearchParams();
    const feed = searchParams.get('feed') || 'following';

    useEffect(() => {
        let mounted = true;
        async function fetchPosts(pageToFetch = 0) {
            if (pageToFetch === 0) setLoading(true);
            else setLoadingMore(true);

            let res
            if (feed === 'following') {
                res = await getMainFeedPostsApi({ page: pageToFetch, size });
            }
            else {
                res = await getExploreFeedPostsApi({ page: pageToFetch, size });
            }
            if (!mounted) return;

            if (res.success) {
                const items = res.data || [];
                setPosts(prev => {
                    const combined = [...prev, ...items];
                    const map = new Map();
                    for (const p of combined) map.set(p.id, p);
                    return Array.from(map.values());
                });

                if (typeof res.totalPages !== 'undefined') {
                    setHasMore(pageToFetch < (res.totalPages - 1));
                } else {
                    setHasMore(items.length === size);
                }
            } else {
                // if (pageToFetch === 0) setPosts(mockPosts);
                showToast("Failed to load posts", res.message, "error");
                setHasMore(false);
            }

            if (mounted) {
                setLoading(false);
                setLoadingMore(false);
            }
        }
        fetchPosts(page);

        return () => { mounted = false; }
    }, [page, size, feed]);

    useEffect(() => {
        setPage(0);
        setPosts([]);
        setHasMore(true);
    }, [feed]);

    useEffect(() => {
        const sentinel = sentinelRef.current;
        if (!sentinel) return;

        const observer = new IntersectionObserver(entries => {
            entries.forEach(entry => {
                if (entry.isIntersecting && hasMore && !loadingMore && !loading) {
                    setPage(p => p + 1);
                }
            });
        });

        observer.observe(sentinel);

        return () => {
            observer.disconnect();
        };
    }, [hasMore, loadingMore, loading]);

    return (
        <div>
            {/* <NavBar /> */}
            <div className="posts-list">
                {loading && <LoadingFallback />}
                {posts.map((post, index) => (
                    <PostCard key={post.id || index} postBody={post} showForumName={true} />
                ))}

                {loadingMore && (
                    <div style={{ marginTop: 20 }}>
                        <LoadingFallback />
                    </div>
                )}

                {/* sentinel for infinite scroll */}
                <div ref={sentinelRef} style={{ height: 1, width: '100%' }} aria-hidden="true" />

                {/* Fallback load more button for older browsers */}
                {hasMore && !loadingMore && (
                    <div style={{ padding: 10 }}>
                        <button
                            className="load-more-btn"
                            onClick={() => setPage(p => p + 1)}
                        >Load more</button>
                    </div>
                )}

                {!hasMore && !loading && <div style={{ padding: 12, color: '#777'}}>No more posts</div>}
            </div>
        </div>
    );
};

export default HomePage;