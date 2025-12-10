import api from './api-client.jsx';
import { mapBackendPostToFrontend } from '../utils/api-mappers.jsx';
import { getMockPosts } from '../data/mock-posts.jsx';
import mockPosts from '../data/mock-posts.jsx';

export async function getPostsApi({ page = 0, size = 10, feed = 'following' } = {}) {
    try {
        const response = await api.get('/post/v1/get-posts', { params: { page, size, feed } });
        const data = response.data;
        if (Array.isArray(data)) {
            return { success: true, data: data.map(mapBackendPostToFrontend), page: page, totalPages: undefined };
        }

        const content = data?.content || [];

        return {
            success: true,
            data: content.map(mapBackendPostToFrontend),
            page: data?.page ?? page,
            totalPages: data?.totalPages
        };
    } catch (err) {
        const mock = getMockPosts({ page, size });
        let content = mock.content;
        if (feed === 'popular') {
            const sorted = mockPosts.slice().sort((a, b) => (b.votes || 0) - (a.votes || 0));
            const start = page * size;
            content = sorted.slice(start, start + size);
        }
        return { success: true, data: content, page: mock.page, totalPages: mock.totalPages };
    }
}

export async function getPostById({ postId }) {
    try {
        const response = await api.get(`/post/v1/get/${postId}`);
        const data = response.data;
        return { success: true, data: mapBackendPostToFrontend(data) };
    } catch (err) {
        return { success: false, message: err.message };
    }
}
