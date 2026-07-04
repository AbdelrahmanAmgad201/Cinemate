import { useCallback, useRef } from 'react';
import { MessageSquareOff } from 'lucide-react';

import './style/PostsFeed.css';
import PostCard from './PostCard.jsx';
import LoadingFallback from './LoadingFallback.jsx';
import EmptyState from './ui/EmptyState.jsx';

export default function PostsFeed({ posts, loading, hasMore, onLoadMore, emptyMessage = 'No posts yet' }) {
    const observer = useRef();

    const lastPostRef = useCallback((node) => {
        if (loading) return;
        if (observer.current) observer.current.disconnect();

        observer.current = new IntersectionObserver((entries) => {
            if (entries[0].isIntersecting && hasMore) onLoadMore();
        });

        if (node) observer.current.observe(node);
    }, [loading, hasMore, onLoadMore]);

    if (!loading && (!posts || posts.length === 0)) {
        return (
            <EmptyState
                icon={<MessageSquareOff size={28} />}
                title={emptyMessage}
            />
        );
    }

    return (
        <div className="posts-feed-container">
            {posts.map((post, index) => (
                index === posts.length - 1
                    ? <div ref={lastPostRef} key={post.postId || post.id}><PostCard postBody={post} /></div>
                    : <PostCard postBody={post} key={post.postId || post.id} />
            ))}

            {loading && <LoadingFallback />}
        </div>
    );
}
