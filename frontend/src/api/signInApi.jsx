import api from './apiClient.jsx';


export default async function signInApi({email, password, role}) {
    try{
        const response = await api.post("/auth/login", {email, password, role});
        const data = response.data;
        const token = data.token;
        console.log(data);
        const user = {
            id: data.id,
            email: data.email,
            role: data.role
        }

        localStorage.setItem('token', token);

        return { success: true, user: user};
    }
    catch(err){
        console.log(err);
        return { success: false , message: err.response?.data?.error };
    }
};
