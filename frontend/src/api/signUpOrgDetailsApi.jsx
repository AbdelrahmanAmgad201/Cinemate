import api from './apiClient.jsx';


export default async function signUpOrgDetailsApi(details) {
    try{

        const response = await api.post("/organization/v1/set-organization-data", details);
        const data = response.data;


        return { success: true };
    }
    catch(err){
        console.log(err);
        return { success: false , message: err.response?.data?.error || err.message };
    }
};
