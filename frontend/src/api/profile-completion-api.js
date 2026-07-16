import api from './api-client.js';
import {setAccessToken} from "../auth/tokenStore.js";


export default async function profileCompletionApi({birthday, gender}) {
    try{
        const response = await api.patch("/user/v1/complete-profile", {birthday, gender});

        const token = response.data;

        setAccessToken(token);

        return { success: true, token: token};
    }
    catch(err){
       console.error('Error completing profile:', err);
       return { success: false , message: err.message };
    }
};
