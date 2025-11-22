import api from './apiClient.jsx';


export default async function signUpUserDetailsApi(details) {
    try{

        const response = await api.post("/user/v1/set-user-data", details);
        const data = response.data;


        return { success: true };
    }
    catch(err){
        console.log(err);
        return { success: false , message: err.response?.data?.error };
    }
};
