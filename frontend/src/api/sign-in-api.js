import api from './api-client.js';
import {setAccessToken} from "../auth/tokenStore.js";


export default async function signInApi({email, password, role}) {
    try{
        const response = await api.post("/auth/v1/login", {email, password, role});
        const data = response.data;
        const token = data.accessToken;
        const user = {
            id: data.id,
            email: data.email,
            role: data.role.replace("ROLE_", "")
        }

        setAccessToken(token);

        return { success: true, user: user};
    }
    catch(err){
        return { success: false , message: err.message };
    }
};
