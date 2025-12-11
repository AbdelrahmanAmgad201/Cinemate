import api from './api-client.jsx';
import {JWT} from "../constants/constants.jsx";


export default async function signInApi({email, password, role}) {
    try{
        const response = await api.post("/auth/v1/login", {email, password, role});
        const data = response.data;
        const token = data.token;
        const user = {
            id: data.id,
            email: data.email,
            role: data.role.replace("ROLE_", "")
        }

        localStorage.setItem(JWT.STORAGE_NAME, token);

        return { success: true, user: user};
    }
    catch(err){
    //     console.log(err);
        return { success: false , message: err.message };
    }
};
