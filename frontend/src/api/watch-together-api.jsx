import api from './api-client.jsx'

export async function createRoomApi({ movieId }) {
    try {

        const response = await api.post(`/watch-party/${movieId}`);

        const partyData = {
            createdAt: response.data.createdAt,
            movieId: response.data.movieId,
            partyId: response.data.partyId,
            status: response.data.status,
            userHostId: response.data.userId
        }

        return {success: true, data: partyData}

    }catch (err) {
        return {success: false, message: err.message};
    }

}

// Data incoming for join and get
// createdAt:"2025-12-23T12:21:32.6518646"
// currentParticipants:2
// hostId:1
// hostName:"Badr"
// members:Array(1):
// {userId: 2, userName: 'Badr'}
// movieId:31
// movieUrl:"0gh0jqtgb0"
// partyId:"073fde55-763b-4486-ade9-d74db75bbc7a"
// status:"ACTIVE"

export async function joinRoomApi({ partyId }) {
    try {

        const response = await api.post(`/watch-party/join/${partyId}`);

        return {success: true, data: {id: response.data.id}}

    }catch (err) {
        return {success: false, message: err.message};
    }
}

export async function getRoomApi({ partyId }) {
    try {

        const response = await api.get(`/watch-party/${partyId}`);

        return {success: true, data: response.data}

    }catch (err) {
        return {success: false, message: err.message};
    }
}

// Guest
export async function leaveRoomApi({ partyId }) {
    try {
        await api.delete(`/watch-party/leave/${partyId}`);

        return { success: true };

    } catch (err) {
        return { success: false, message: err.message };
    }
}

// Host
export async function deleteRoomApi({ partyId }) {
    try {
        await api.delete(`/watch-party/${partyId}`);

        return { success: true };

    } catch (err) {
        return { success: false, message: err.message };
    }
}