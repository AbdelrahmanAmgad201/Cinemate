import React, { useEffect, useState } from 'react';
import ForumCard from '../../components/ForumCard.jsx';
import '../../components/style/forumCard.css';
import './style/exploreForums.css';

import { getExploreForumsApi } from '../../api/explore-api.jsx';
import { followForumApi, unfollowForumApi } from '../../api/forums-api.jsx';

export default function ExploreForums(){
    const [sections, setSections] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let mounted = true;
        async function load() {
            setLoading(true);
            const res = await getExploreForumsApi();
            if (!mounted) return;
            if (res.success) {
                const raw = (() => {
                    try { return JSON.parse(localStorage.getItem('mock_followed_forums') || '[]'); } catch(e) { return []; }
                })();

                const sectionsWithFollow = res.data.map(section => ({
                    ...section,
                    forums: (section.forums || []).map(f => ({ ...f, followed: raw.includes(f.id) }))
                }));

                setSections(sectionsWithFollow);
            }
            setLoading(false);
        }

        load();
        return () => { mounted = false };
    }, []);

    async function handleFollowToggle(id, followed) {
        setSections(prev => prev.map(section => ({
            ...section,
            forums: section.forums.map(f => f.id === id ? { ...f, followed, followerCount: (followerCountOrZero(f.followers ?? f.followerCount) + (followed ? 1 : -1)) } : f)
        })));

        try {
            if (followed) await followForumApi({ forumId: id });
            else await unfollowForumApi({ forumId: id });
        } catch (e) {
            // ignored: APIs already return success on fallback
        }
    }

    function followerCountOrZero(v) {
        if (v == null) return 0;
        return Number(v) || 0;
    }

    if (loading) {
        return (
            <main className="explore-page">
                <div className="explore-container">
                    <h1 className="explore-title">Explore Forums</h1>
                    <p>Loading...</p>
                </div>
            </main>
        )
    }

    return (
        <main className="explore-page">
            <div className="explore-container">
                <h1 className="explore-title">Explore Forums</h1>

                {sections.map(section => (
                    <section key={section.category} className="explore-section">
                        <h3 className="section-title">{section.category}</h3>
                        <div className="forums-grid">
                            {section.forums.map(f => (
                                <ForumCard key={f.id} forum={f} onFollowChange={handleFollowToggle} />
                            ))}
                        </div>
                    </section>
                ))}
            </div>
        </main>
    )
}

