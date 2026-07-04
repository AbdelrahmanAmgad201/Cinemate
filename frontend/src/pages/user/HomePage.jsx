import { useState, useEffect, useContext } from 'react';
import { useSearchParams } from 'react-router-dom';
import PostsFeed from '../../components/PostsFeed.jsx';
import { getExploreFeedPostsApi, getMainFeedPostsApi } from '../../api/posts-api.js';
import './style/HomePage.css';
import { ToastContext } from '../../context/ToastContext.jsx';

const PAGE_SIZE = 10;

const HomePage = () => {
    const [posts, setPosts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);

    const { showToast } = useContext(ToastContext);

    const [searchParams] = useSearchParams();
    const feed = searchParams.get('feed') || 'following';

    useEffect(() => {
        let mounted = true;

        async function fetchPosts(pageToFetch) {
            setLoading(true);

            const res = feed === 'following'
                ? await getMainFeedPostsApi({ page: pageToFetch, size: PAGE_SIZE })
                : await getExploreFeedPostsApi({ page: pageToFetch, size: PAGE_SIZE });

            if (!mounted) return;

            if (res.success) {
                const items = res.data || [];
                setPosts((prev) => {
                    const combined = pageToFetch === 0 ? items : [...prev, ...items];
                    const map = new Map();
                    for (const p of combined) map.set(p.id, p);
                    return Array.from(map.values());
                });

                setHasMore(typeof res.totalPages !== 'undefined'
                    ? pageToFetch < res.totalPages - 1
                    : items.length === PAGE_SIZE);
            } else {
                showToast('Failed to load posts', res.message, 'error');
                setHasMore(false);
            }

            setLoading(false);
        }

        fetchPosts(page);
        return () => { mounted = false; };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [page, feed]);

    useEffect(() => {
        setPage(0);
        setPosts([]);
        setHasMore(true);
    }, [feed]);

    return (
        <div className="home-page">
            <PostsFeed
                posts={posts}
                loading={loading}
                hasMore={hasMore}
                onLoadMore={() => setPage((p) => p + 1)}
                emptyMessage={feed === 'following' ? "Follow some forums to see posts here" : 'No posts yet'}
            />
        </div>
    );
};

export default HomePage;
