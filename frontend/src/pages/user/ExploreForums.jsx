import React, { useEffect, useState, useCallback } from 'react';
import ForumCard from '../../components/ForumCard.jsx';
import '../../components/style/forumCard.css';
import './style/exploreForums.css';

import { getExploreForumsApi } from '../../api/explore-api.jsx';

const SORT_OPTIONS = [
    { label: "Newest", value: "new" },
    { label: "Most Followed", value: "followers" },
    { label: "Most Active", value: "posts" },
];

export default function ExploreForums() {
    const [forums, setForums] = useState([]);
    const [loading, setLoading] = useState(false);
    const [currentSort, setCurrentSort] = useState("new");

    // Pagination State
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);

    const fetchForums = useCallback(async (pageToFetch, shouldAppend = false) => {
        setLoading(true);

        const res = await getExploreForumsApi({
            page: pageToFetch,
            size: 5,
            sort: currentSort
        });

        if (res.success) {
            const newForums = res.data || [];

            setForums(prev => {
                return shouldAppend ? [...prev, ...newForums] : newForums;
            });

            if (newForums.length < 5) {
                setHasMore(false);
            } else {
                setHasMore(true);
            }
        } else {
            console.error(res.message);
        }
        setLoading(false);
    }, [currentSort]);

    useEffect(() => {
        setPage(0);
        setForums([]);
        fetchForums(0, false);
    }, [currentSort, fetchForums]);

    const handleLoadMore = () => {
        const nextPage = page + 1;
        setPage(nextPage);
        fetchForums(nextPage, true);
    };

    return (
        <main className="explore-page">
            <div className="explore-container">
                <div className="explore-header">
                    <h1 className="explore-title">Explore Forums</h1>

                    <div className="explore-sort-buttons">
                        {SORT_OPTIONS.map((option) => (
                            <button
                                key={option.value}
                                className={`sort-btn ${currentSort === option.value ? 'active' : ''}`}
                                onClick={() => setCurrentSort(option.value)}
                            >
                                {option.label}
                            </button>
                        ))}
                    </div>
                </div>

                <div className="forums-grid">
                    {forums.map(forum => (
                        <ForumCard key={forum.id} forum={forum} />
                    ))}
                </div>

                {!loading && forums.length === 0 && (
                    <p style={{ textAlign: 'center', color: '#888', marginTop: 40 }}>
                        No forums found.
                    </p>
                )}

                {loading && (
                    <p style={{ textAlign: 'center', color: '#888', marginTop: 20 }}>
                        Loading...
                    </p>
                )}

                {!loading && hasMore && forums.length > 0 && (
                    <div className="load-more-container">
                        <button className="load-more-btn" onClick={handleLoadMore}>
                            Load More Forums
                        </button>
                    </div>
                )}

                {!loading && !hasMore && forums.length > 0 && (
                    <p style={{ textAlign: 'center', color: '#555', margin: '30px 0' }}>
                        ~ end of results ~
                    </p>
                )}
            </div>
        </main>
    );
}