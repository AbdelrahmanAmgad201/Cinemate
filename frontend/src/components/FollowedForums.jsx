import { useState, useEffect } from 'react';
import { getFollowedForumsApi } from '../api/forums-api.js';
import './style/FollowedForums.css';
import { Link } from 'react-router-dom';
import { PATHS } from '../constants/constants.jsx';
import Avatar from './ui/Avatar.jsx';
import Skeleton from './ui/Skeleton.jsx';

const DEFAULT_VISIBLE = 5;

const FollowedForums = ({ maxVisible = DEFAULT_VISIBLE }) => {
    const [forums, setForums] = useState([]);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(false);
    const [visibleCount, setVisibleCount] = useState(maxVisible);
    const [isLoading, setIsLoading] = useState(true);

    const fetchPage = async (pageToFetch = 0) => {
        setIsLoading(true);
        const res = await getFollowedForumsApi({ page: pageToFetch, size: maxVisible });
        setIsLoading(false);
        if (res?.success) {
            const raw = res.data.forums || res.data.content || res.data || [];
            const list = Array.isArray(raw) ? raw : [];
            const currentTotal = pageToFetch === 0 ? 0 : forums.length;
            const newTotal = currentTotal + list.length;

            setForums(prev => (pageToFetch === 0 ? list : [...prev, ...list]));
            setPage(pageToFetch);
            setHasMore(list.length === maxVisible);
            setVisibleCount(prev => {
                if (pageToFetch === 0) return Math.min(maxVisible, list.length);
                return Math.min(prev + maxVisible, newTotal);
            });

            return { newTotal, loaded: list.length };
        }
        return { newTotal: forums.length, loaded: 0 };
    };

    useEffect(() => {
        fetchPage(0);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [maxVisible]);

    const handleShowMore = async () => {
        if (forums.length > visibleCount) {
            setVisibleCount(prev => Math.min(prev + maxVisible, forums.length));
            return;
        }
        if (hasMore) {
            const { newTotal } = await fetchPage(page + 1);
            setVisibleCount(prev => Math.min(prev + maxVisible, newTotal));
            return;
        }
        setVisibleCount(maxVisible);
    };

    const remainingLoaded = Math.max(0, forums.length - visibleCount);
    let showMoreLabel;
    if (isLoading) showMoreLabel = '';
    else if (remainingLoaded > 0) showMoreLabel = `Show ${Math.min(maxVisible, remainingLoaded)} more`;
    else if (hasMore) showMoreLabel = `Show ${maxVisible} more`;
    else showMoreLabel = '';

    const handleCollapse = () => setVisibleCount(maxVisible);

    const displayedForums = (() => {
        const total = Math.min(visibleCount, forums.length);
        const groups = Math.ceil(total / maxVisible);
        let out = [];
        for (let i = 0; i < groups; i++) {
            const start = i * maxVisible;
            const end = Math.min(start + maxVisible, total);
            out = out.concat(forums.slice(start, end).reverse());
        }
        return out;
    })();

    const showCollapseVisible = visibleCount > maxVisible;

    return (
        <div className="followed-forums">
            <div className="followed-forums-header">Followed forums</div>

            {isLoading && (
                <div className="followed-forums-list">
                    {Array.from({ length: 3 }).map((_, i) => (
                        <div className="followed-forum-item" key={i}>
                            <Skeleton variant="circle" width={28} height={28} />
                            <Skeleton variant="text" width="70%" />
                        </div>
                    ))}
                </div>
            )}

            {!isLoading && forums.length === 0 && (
                <p className="followed-forums-empty">Forums you follow will show up here.</p>
            )}

            {!isLoading && forums.length > 0 && (
                <div className="followed-forums-list">
                    {displayedForums.map(f => (
                        <Link to={PATHS.FORUM.PAGE(f.id)} key={f.id} className="followed-forum-item" title={f.name}>
                            <Avatar name={f.name} src={f.avatar} size="xs" />
                            <span className="forum-name">{f.name}</span>
                        </Link>
                    ))}
                </div>
            )}

            {(showMoreLabel || showCollapseVisible) && (
                <div className="show-controls">
                    {showMoreLabel && (
                        <button className="show-more" onClick={handleShowMore} disabled={isLoading}>{showMoreLabel}</button>
                    )}
                    {showCollapseVisible && (
                        <button className="collapse" onClick={handleCollapse}>Collapse</button>
                    )}
                </div>
            )}
        </div>
    );
};

export default FollowedForums;
