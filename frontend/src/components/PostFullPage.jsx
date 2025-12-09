import React from 'react';
import { useParams } from 'react-router-dom';

const PostFullPage = () => {
    const { postId } = useParams();

    return (
        <h1>POST ID: {postId}</h1>
    );
};

export default PostFullPage;