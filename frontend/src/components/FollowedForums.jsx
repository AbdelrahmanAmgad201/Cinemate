import React, { useState, useEffect } from 'react';
// import followedForumsMock from '../data/followed-forums.jsx';
import { getFollowedForumsApi } from '../api/forums-api.jsx';
import './style/FollowedForums.css';
import { Link } from 'react-router-dom';
import {PATHS} from "../constants/constants.jsx";

const DEFAULT_VISIBLE = 5;

const FollowedForums = ({ maxVisible = DEFAULT_VISIBLE }) => {
  const [forums, setForums] = useState([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [visibleCount, setVisibleCount] = useState(maxVisible);
  const [isLoading, setIsLoading] = useState(false);

  const fetchPage = async (pageToFetch = 0) => {
    setIsLoading(true);
    const res = await getFollowedForumsApi({ page: pageToFetch, size: maxVisible });
    setIsLoading(false);
    if (res?.success) {
      const raw = res.data.forums || res.data.content || res.data || [];
      const list = Array.isArray(raw) ? raw : [];
      // compute new total before updating state to avoid race conditions
      const currentTotal = pageToFetch === 0 ? 0 : forums.length;
      const newTotal = currentTotal + list.length;

      setForums(prev => pageToFetch === 0 ? list : [...prev, ...list]);
      setPage(pageToFetch);
      setHasMore(list.length === maxVisible);
      // update visibleCount based on the computed new total
      setVisibleCount(prev => {
        if (pageToFetch === 0) return Math.min(maxVisible, list.length);
        return Math.min(prev + maxVisible, newTotal);
      });

      return { newTotal, loaded: list.length };
    }
    return { newTotal: forums.length, loaded: 0 };
  };

  useEffect(() => {
    let mounted = true;
    async function init(){
      await fetchPage(0);
    }
    init();
    return () => { mounted = false; }
  }, [maxVisible]);

  const handleShowMore = async () => {
    // if there are already-loaded but hidden items, reveal them
    if (forums.length > visibleCount) {
      setVisibleCount(prev => Math.min(prev + maxVisible, forums.length));
      return;
    }
    // else try to load next page if available
    if (hasMore) {
      const { newTotal } = await fetchPage(page + 1);
      setVisibleCount(prev => Math.min(prev + maxVisible, newTotal));
      return;
    }
    // else collapse back to initial view
    setVisibleCount(maxVisible);
  };

  // compute label for show-more button
  const remainingLoaded = Math.max(0, forums.length - visibleCount);
  let showMoreLabel;
  if (isLoading) showMoreLabel = 'Loading...';
  else if (remainingLoaded > 0) showMoreLabel = `Show ${Math.min(maxVisible, remainingLoaded)} more ▼`;
  else if (hasMore) showMoreLabel = `Show ${maxVisible} more ▼`;
  else showMoreLabel = 'Show less ▲';

  const handleCollapse = () => setVisibleCount(maxVisible);

  // prepare displayed forums: reverse order inside each page-sized group
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

  return (
    <div className="followed-forums">
      <div className="followed-forums-header">Followed forums</div>
      <div className="followed-forums-list">
        {displayedForums.map(f => (
          <Link to={PATHS.FORUM.PAGE(f.id)} key={f.id} className="followed-forum-item" title={f.name}>
            <div className="forum-avatar">
              {f.avatar ? <img src={f.avatar} alt={f.name} /> : <div className="forum-initials">{f.name.split(' ').map(n => n[0]).slice(0,2).join('')}</div>}
            </div>
            <div className="forum-name">{f.name}</div>
          </Link>
        ))} 
      </div>
      {(forums.length > maxVisible || hasMore) && (
        <div className="show-controls">
          <button className="show-more" onClick={handleShowMore} disabled={isLoading}>
            {showMoreLabel}
          </button>
          {visibleCount > maxVisible && (
            <button className="collapse" onClick={handleCollapse}>Collapse ▲</button>
          )}
        </div>
      )}
    </div>
  );
};

export default FollowedForums;