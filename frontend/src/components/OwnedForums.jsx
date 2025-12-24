import React, { useState, useEffect, useRef, useCallback } from 'react';
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

  const fetchPage = useCallback(async (pageToFetch = 0) => {
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
  }, [pageSize]);

  useEffect(() => {
    (async () => {
      await fetchPage(0);
    })();
  }, [fetchPage]);

  const sentinelRef = useRef(null);

  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel) return;
    const observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting && hasMore && !isLoading) {
        fetchPage(page + 1);
      }
    }, { root: null, rootMargin: '200px', threshold: 0.1 });

    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [hasMore, isLoading, page, fetchPage]);

  if (isLoading && forums.length === 0) return <div>Loading forums...</div>;

  if (error) return <div className="owned-forums-error">Failed to load forums: {error}</div>;
  if (!isLoading && forums.length === 0) return <div className="owned-forums-empty">You have not created any forums yet.</div>;

  return (
    <div className="owned-forums">
      <div className="owned-forums-list">
        {forums.map(f => (
          <ForumCard key={f.id} forum={f} />
        ))}
      </div>

      <div ref={sentinelRef} className="owned-forums-sentinel" />

      {isLoading && forums.length > 0 && (
        <div className="owned-forums-loading">Loading more...</div>
      )}
    </div>
  );
}
