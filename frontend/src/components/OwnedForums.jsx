import React, { useState, useEffect } from 'react';
import { getUserForumsApi } from '../api/forums-api.jsx';
import ForumCard from './ForumCard.jsx';
import './style/OwnedForums.css';

const PAGE_SIZE = 8;

export default function OwnedForums({ pageSize = PAGE_SIZE }) {
  const [forums, setForums] = useState([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchPage = async (pageToFetch = 0) => {
    setIsLoading(true);
    setError(null);
    const res = await getUserForumsApi({ page: pageToFetch, size: pageSize });
    setIsLoading(false);
    if (res?.success) {
      const raw = res.data.content || [];
      const list = Array.isArray(raw) ? raw : [];
      if (pageToFetch === 0) setForums(list);
      else setForums(prev => [...prev, ...list]);
      setPage(pageToFetch);
      setHasMore(list.length === pageSize);
      return { loaded: list.length };
    }
    setError(res?.message || 'Failed to load forums');
    return { loaded: 0 };
  };

  useEffect(() => {
    let mounted = true;
    (async () => {
      await fetchPage(0);
    })();
    return () => { mounted = false; }
  }, [pageSize]);

  const handleShowMore = async () => {
    if (hasMore) {
      await fetchPage(page + 1);
      return;
    }
  };

  if (isLoading && forums.length === 0) return <div>Loading forums...</div>;

  if (error) return <div className="owned-forums-error">Failed to load forums: {error}</div>;
  if (!isLoading && forums.length === 0) return <div className="owned-forums-empty">You have not created any forums yet.</div>;

  return (
    <div className="owned-forums">
      <div className="owned-forums-header">My forums</div>
      <div className="owned-forums-list">
        {forums.map(f => (
          <ForumCard key={f.id} forum={f} />
        ))}
      </div>

      {hasMore && (
        <div className="owned-forums-actions">
          <button className="show-more" onClick={handleShowMore} disabled={isLoading}>{isLoading ? 'Loading...' : 'Show more'}</button>
        </div>
      )}
    </div>
  );
}
