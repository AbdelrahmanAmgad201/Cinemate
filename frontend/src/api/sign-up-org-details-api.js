import api from './api-client.js';


export default async function signUpOrgDetailsApi(details) {
    try{

        await api.put("/organization/v1/profile", details);

        return { success: true };
    }
    catch(err){
        return { success: false , message: err.response?.data?.error || err.message };
    }
};
