import api from './apiClient.jsx';


export default async function signUpOrgDetails({ }) {
    try{
        // TODO: add correct api endpoint
        const response = await api.post("/auth/v1/...", {});
        const data = response.data;


        return { success: true };
    }
    catch(err){
        console.log(err);
        return { success: false , message: err.response?.data?.error };
    }
};
