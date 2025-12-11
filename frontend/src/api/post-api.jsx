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
