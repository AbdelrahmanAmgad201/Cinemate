import api from './api-client.js';


export default async function signUpUserDetailsApi(details) {
    try{

        await api.put("/user/v1/profile-data", details);

        return { success: true };
    }
    catch(err){
        return { success: false , message: err.message };
    }
};
