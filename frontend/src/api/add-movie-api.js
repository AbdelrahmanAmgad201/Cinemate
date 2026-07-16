import api from './api-client.js';

export default async function addMovieApi (movieData){
    try{
        const response = await api.post("/organization/v1/movies", movieData);
        return { success: true, message: response.data };
    }catch(err){
        return { success: false , message: err.message };
    }
}