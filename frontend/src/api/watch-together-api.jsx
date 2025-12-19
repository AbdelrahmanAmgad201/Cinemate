import api from './api-client.jsx'

// Either take a movieId or nothing -> Depends on whether we can add movies to the room or not
// TODO:
export async function createRoomApi({ movieId }) {
    try {

        const response = await api.post("/watch-together/v1/create-room", {movieId});

        return {success: true, data: {id: response.data.id}}

    }catch (err) {
        return {success: false, message: err.message};
    }

}

export async function addMovieToRoom({ }) {}

export async function joinRoomApi({ code }) {
    try {

        const response = await api.post("/watch-together/v1/join-room", {code});

        return {success: true, data: {id: response.data.id}}

    }catch (err) {
        return {success: false, message: err.message};
    }
}

export async function leaveRoom({ }) {}

