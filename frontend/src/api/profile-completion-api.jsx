import api from './api-client.jsx';
import {JWT} from "../constants/constants.jsx";


export default async function profileCompletionApi({birthday, gender}) {
    try{
        const response = await api.patch("/user/v1/complete-profile", {birthday, gender});
        
        const token = response.data;

        sessionStorage.setItem(JWT.STORAGE_NAME, token);

        return { success: true, token: token};
    }
    catch(err){
       console.log(err);
       return { success: false , message: err.message };
    }
};
