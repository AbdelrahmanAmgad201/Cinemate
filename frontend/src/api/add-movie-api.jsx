import api from './api-client.jsx';

export default async function addMovieApi (movieData){
    try{
        const response = await api.post("/organization/v1/add-movie", movieData);
        return { success: true, message: response.data };
    }catch(err){
        // console.log(err);
        return { success: false , message: err.message };
    }
}