import api from "./api-client.jsx";

export async function addPostApi({ forumId, title, content }) {
    try {
        const response = await api.post(`/post/v1/post`, { forumId, title, content });
        console.log(response.data);

        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}
