import React from 'react';
import { useParams } from 'react-router-dom';

export default function ForumPage() {
    const { forumId } = useParams();

    return (
        <div>
            <h1>Forum Page - ID: {forumId}</h1>
            <p>Forum page content will be implemented here.</p>
        </div>
    );
}
