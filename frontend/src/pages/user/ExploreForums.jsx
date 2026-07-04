import { useEffect, useState, useCallback } from 'react';
import { Compass } from 'lucide-react';
import ForumCard from '../../components/ForumCard.jsx';
import '../../components/style/forumCard.css';
import './style/exploreForums.css';

import { getExploreForumsApi } from '../../api/explore-api.js';
import Button from '../../components/ui/Button.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';

const SORT_OPTIONS = [
    { label: 'Newest', value: 'new' },
    { label: 'Most followed', value: 'followers' },
    { label: 'Most active', value: 'posts' },
];

const PAGE_SIZE = 12;

export default function ExploreForums() {
    const [forums, setForums] = useState([]);
    const [loading, setLoading] = useState(false);
    const [currentSort, setCurrentSort] = useState('new');

    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);

    const fetchForums = useCallback(async (pageToFetch, shouldAppend = false) => {
        setLoading(true);

        const res = await getExploreForumsApi({ page: pageToFetch, size: PAGE_SIZE, sort: currentSort });

        if (res.success) {
            const newForums = res.data || [];
            setForums((prev) => (shouldAppend ? [...prev, ...newForums] : newForums));
            setHasMore(newForums.length >= PAGE_SIZE);
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
                    <h1 className="explore-title">Explore forums</h1>

                    <div className="explore-sort-buttons">
                        {SORT_OPTIONS.map((option) => (
                            <button
                                key={option.value}
                                type="button"
                                className={`sort-btn ${currentSort === option.value ? 'active' : ''}`}
                                onClick={() => setCurrentSort(option.value)}
                            >
                                {option.label}
                            </button>
                        ))}
                    </div>
                </div>

                {loading && forums.length === 0 && (
                    <div className="forums-grid">
                        {Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} variant="rect" height={88} />)}
                    </div>
                )}

                {!loading && forums.length === 0 && (
                    <EmptyState icon={<Compass size={28} />} title="No forums found" description="Try a different sort order, or check back later." />
                )}

                {forums.length > 0 && (
                    <div className="forums-grid">
                        {forums.map((forum) => <ForumCard key={forum.id} forum={forum} />)}
                    </div>
                )}

                {hasMore && forums.length > 0 && (
                    <div className="load-more-container">
                        <Button variant="secondary" onClick={handleLoadMore} loading={loading}>Load more forums</Button>
                    </div>
                )}

                {!loading && !hasMore && forums.length > 0 && (
                    <p className="explore-end-of-results">You've reached the end</p>
                )}
            </div>
        </main>
    );
}
