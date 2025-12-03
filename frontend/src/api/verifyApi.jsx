import api from './apiClient.jsx';
import {JWT} from "../constants/constants.jsx";


export default async function verifyApi({email, code}) {
    try{
        // TODO: add correct verify api endpoint

        const response = await api.post("/verification/v1/verify", {email, code});
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
        // console.log(err);
        return { success: false , message: err.message };
    }
};
