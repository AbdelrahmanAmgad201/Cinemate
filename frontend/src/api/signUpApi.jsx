import api from './apiClient.jsx';


export default async function signUpApi({email, password, role}) {
    try{
        const response = await api.post("/auth/v1/sign-up", {email, password, role});
        const data = response.data;

        const user = {email:email, role:role}

        return { success: true , user: user };
    }
    catch(err){
        console.log(err);
        return { success: false , message: err.response?.data?.error };
    }
};
