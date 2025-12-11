import api from "./api-client.jsx";

// Tested
export async function createForumApi({ name, description }) {
    try {
        const response = await api.post(`/forum/v1/create`, { name, description });
        console.log(response.data);

        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

// Tested
export async function deleteForumApi({ forumId }) {
    try {
        const response = await api.delete(`/forum/v1/delete/${forumId}`);
        console.log(response.data);

        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

// Tested
export async function updateForumApi({ forumId, name, description }) {
    try {
        const response = await api.put(`/forum/v1/update/${forumId}`, {
            name,
            description
        });
        console.log(response.data);

        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}


// Tested
export async function unfollowForumApi({ forumId }) {
    try {
        const response = await api.delete(`/forum-follow/v1/follow/${forumId}`);
        console.log(response.data);

        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

// Tested
export async function followForumApi({ forumId }) {
    try {
        const response = await api.put(`/forum-follow/v1/follow/${forumId}`);
        console.log(response.data);

        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

// TODO:
export async function getForumApi({ forumId }) {
    try {
        const response = await api.get(`/forum/v1/get-forum-by-id/${forumId}`);
        console.log(response.data);

        const normalId = parseInt(response.data.ownerId, 16);

        const data = {
            id: response.data.id,
            name: response.data.name,
            description: response.data.description,
            createdAt: response.data.createdAt,
            followerCount: response.data.followerCount,
            postCount: response.data.postCount,
            ownerId24Bit: response.data.ownerId, // TODO: THIS HAS ISSUES
            ownerId: normalId,
        }
        console.log(data);
        return { success: true, data: data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

//TODO: I want a check follow endpoint
export async function checkFollowApi({ forumId }) {
    try {
        const response = await api.get(`/forum-follow/v1/is-followed/${forumId}`);
        console.log(response.data);

        return { success: true, data: response.data }; // true or false
    } catch (err) {
        return { success: false, message: err.message };
    }
}

// TODO: Get a forum's posts -> 
export async function getForumPostsApi({ forumId, page, size }) {
    try {
        const response = await api.post(`/post/v1/forum-posts`, {page, pageSize: size, forumId});
        console.log(response);
        const data = {
            posts: response.data.content,
            // size: response.data.size,
        }
        return { success: true, data: data };
    } catch (err) {
        return { success: false, message: err.message };
    }

}

export async function getModApi({userId}) {
    try {
        const response = await api.get(`/user/v1/user-name-from-object-user-id/${userId}`);
        console.log(response);

        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}







// TODO: Get followed forums
// TODO: Get some forums based on some criteria

