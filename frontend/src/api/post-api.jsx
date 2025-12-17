import api from './api-client.jsx';


export async function updatePostApi({postId, forumId, title, content}) {
    try{
        const response = await api.put(`/post/v1/post/${postId}`, {forumId, title, content});
        const data = response.data;

        console.log(data);
        return { success: true, data: data};
    }
    catch(err){
    //     console.log(err);
        return { success: false , message: err.message };
    }
}

export async function deletePostApi({postId}) {
    if (!postId) {
        return { success: false, message: 'Invalid postId' };
    }
    try{
        const response = await api.delete(`/post/v1/post/${postId}`);
        const data = response.data;

        console.log(data);
        return { success: true, data: data};
    }
    catch(err){
    //     console.log(err);
        return { success: false , message: err.message };
    }
}

export async function votePostApi({postId, value}) {
    try{
        const response = await api.post("/vote/v1/post-vote", {
            targetId: postId,
            value: value
        });
        const data = response.data;

        console.log(data);
        return { success: true, data: data};
    }
    catch(err){
    //     console.log(err);
        return { success: false , message: err.message };
    }
}

export async function updateVotePostApi({ postId, value }) {
    try{
        const response = await api.put("/vote/v1/update-vote", {
            targetId: postId, 
            value: value});
        const data = response.data;

        console.log(data);
        return { success: true, data: data};
    }
    catch(err){
    //     console.log(err);
        return { success: false , message: err.message };
    }
}

export async function deleteVotePostApi({ targetId }) {
    try{
        const response = await api.delete(`/vote/v1/delete-vote/${targetId}`);
        const data = response.data;

        console.log(data);
        return { success: true, data: data};
    }
    catch(err){
    //     console.log(err);
        return { success: false , message: err.message };
    }
}

export async function isVotedPostApi({ targetId }) {
    try{
        const response = await api.get(`/vote/v1/is-voted/${targetId}`);
        const data = response.data;

        // console.log(data);
        return { success: true, data: data};
    }
    catch(err){
    //     console.log(err);
        return { success: false , message: err.message };
    }
}

export async function getPostApi({ postId }) {
    try {
        const response = await api.get(`/post/v1/post/${postId}`);
        const data = response.data;

        return { success: true, data: data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function addPostApi({ forumId, title, content }) {
    try {
        const response = await api.post(`/post/v1/post`, { forumId, title, content });
        console.log(response.data);

        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}
