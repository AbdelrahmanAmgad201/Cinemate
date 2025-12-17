import {useCallback, useRef} from "react";


import "./style/PostsFeed.css";
import emptySetIcon from "../assets/icons/empty-set.png"
import PostCard from "./PostCard.jsx";
import LoadingFallback from "./LoadingFallback.jsx";

export default function PostsFeed({ posts, loading, hasMore, onLoadMore, emptyMessage = "No posts yet"}) {

    const observer = useRef();

    const lastPostRef = useCallback((node) => {
        if (loading) return;

        if (observer.current) observer.current.disconnect();

        observer.current = new IntersectionObserver((entries) => {
            // load more if the element is visible and there are more data to laod
            if (entries[0].isIntersecting && hasMore) onLoadMore();
        });

        if (node) observer.current.observe(node);

    }, [loading, hasMore, onLoadMore]);


    if (!loading && (!posts || posts.length === 0)) {
        return (
            <div className="posts-feed-empty" style={{display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: "20px"}}>
                <img src={emptySetIcon} className="empty-icon" style={{width: "40px", height: "40px"}}></img>
                <p>{emptyMessage}</p>
            </div>
        );
    }

    return (
        <div className="posts-feed-container">
            {posts.map((post, index) => {
                if (index === posts.length - 1)
                    return (
                        <div ref={lastPostRef} key={post.postId || post.id}>
                            <PostCard postBody={post} />
                        </div>
                    )

                return <PostCard postBody={post} key={post.postId || post.id} />
            })}

            {loading && (
                <LoadingFallback />
            )}
        </div>
    )
}