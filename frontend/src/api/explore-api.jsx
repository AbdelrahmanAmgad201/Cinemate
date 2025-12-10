import api from './api-client.jsx';
import { mapBackendForumToFrontend } from '../utils/api-mappers.jsx';
import exploreMock from '../data/explore-forums-mock.jsx';

export async function getExploreForumsApi() {
    try {
        const res = await api.get('/forum/v1/explore');
        const data = res.data;

        if (Array.isArray(data)) {
            const mapped = data.map(section => ({
                category: section.category,
                forums: (section.forums || []).map(mapBackendForumToFrontend)
            }));

            return { success: true, data: mapped };
        }

        const content = data?.content || [];
        const mapped = content.map(section => ({
            category: section.category,
            forums: (section.forums || []).map(mapBackendForumToFrontend)
        }));

        return {
            success: true,
            data: mapped,
            page: data?.page,
            totalPages: data?.totalPages
        };
    } catch (err) {
        return { success: true, data: exploreMock };
    }
}
