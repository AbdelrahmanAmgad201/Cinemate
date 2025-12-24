import React, { useState, useEffect, useContext } from 'react';
import { getMyPostsApi, getOtherUserPostsApi } from '../api/posts-api.jsx';
import PostCard from './PostCard.jsx';
import { ToastContext } from '../context/ToastContext.jsx';

export default function UserPosts({ userId, isOwnProfile, active, pageSize = 20, isPrivateProfile = false }) {
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const { showToast } = useContext(ToastContext);

  const getFriendlyPostsError = (res) => {
    if (!res) return 'Unknown error';
    const msg = String(res.message || (res.raw?.response?.data?.message) || '');
    if (/getIsPublic|Cannot invoke.*booleanValue|this profile is private/i.test(msg)) {
      return 'This profile is private or unavailable.';
    }
    if (res.status === 401) return 'Sign in required to view this content.';
    return msg || 'Unknown error';
  };

  useEffect(() => {
    if (active !== 'posts') return;
    if (isPrivateProfile && !isOwnProfile) {
      setPosts([]);
      setError('This profile is private or unavailable.');
      setLoading(false);
      return;
    }

    let ignore = false;
    const isNumericId = typeof userId === 'string' && /^\d+$/.test(userId);

    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        if (isOwnProfile) {
          const res = await getMyPostsApi({ page: 0, size: pageSize });
          if (!ignore) {
            if (res?.success) setPosts(res.data || []);
            else {
              console.error('[UserPosts] getMyPosts failed', res);
              setPosts([]);
              setError(getFriendlyPostsError(res));
              showToast('Failed to load posts', getFriendlyPostsError(res), 'error');
            }
          }
        } else if (isNumericId) {
          const res = await getOtherUserPostsApi({ userId: Number(userId), page: 0, size: pageSize });
          if (!ignore) {
            if (res?.success) setPosts(res.data || []);
            else {
              console.error('[UserPosts] getOtherUserPosts failed', res);
              setPosts([]);
              setError(getFriendlyPostsError(res));
              showToast('Failed to load posts', getFriendlyPostsError(res), 'error');
            }
          }
        } else {
          if (!ignore) setPosts([]);
        }
      } catch (err) {
        if (!ignore) {
          setError(err?.message || 'Unknown error');
          showToast('Failed to load posts', err?.message || 'Unknown error', 'error');
        }
      } finally {
        if (!ignore) setLoading(false);
      }
    };

    load();
    return () => { ignore = true; };
  }, [active, userId, isOwnProfile, pageSize, showToast, isPrivateProfile]);

  if (loading) return <div>Loading posts...</div>;
  if (error) return <p className="placeholder-note">Failed to load posts: {error}</p>;
  if (!posts || posts.length === 0) return <p className="placeholder-note">No posts found.</p>;

  return (
    <div className="posts-list">
      {posts.map(p => (
        <PostCard key={p.id || p.postId} postBody={p} />
      ))}
    </div>
  );
}
