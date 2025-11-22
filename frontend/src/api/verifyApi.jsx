import api from './apiClient.jsx';


export default async function verifyApi({email, code}) {
    try{
        // TODO: add correct verify api endpoint
        const response = await api.post("/auth/v1/verfiy", {email, code});
        const data = response.data;


        return { success: true };
    }
    catch(err){
        console.log(err);
        return { success: false , message: err.response?.data?.error };
    }
};
