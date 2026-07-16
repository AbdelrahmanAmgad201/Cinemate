import api from './api-client.js';

export async function getRequestsHistoryApi() {
    try{
        const response = await api.get("/admin/v1/my-requests");
        const rawRequestsHistory = response.data;
        const RequestsHistoryMapped = rawRequestsHistory.map(req => ({
            id: req.id,
            movieName: req.movieName,
            state: req.state,
            createdAt: req.createdAt,
            stateUpdatedAt: req.stateUpdatedAt,
            movie: { movieID: req.movieId, name: req.movieName},
            admin: req.admin,
            organizationName: req.organization,
        }));

        return { success: true, data: RequestsHistoryMapped};
    }
    catch(err){
        return { success: false , message: err.message };
    }
}

export async function declineRequestApi({requestId}) {
    try{
        const response = await api.post(`/admin/v1/requests/${requestId}/decline`);
        return { success: true, data: response.data};
    }
    catch(err){
        return { success: false , message: err.message };
    }
}

export async function acceptRequestApi({requestId}) {
    try{
        const response = await api.post(`/admin/v1/requests/${requestId}/accept`);
        return { success: true, data: response.data};
    }
    catch(err){
        return { success: false , message: err.message };
    }
}

export async function getPendingRequestsApi() {

    try{
        const response = await api.get("/admin/v1/pending-requests");
        const pendingRequests = response.data;
        const pendingRequestsArray = pendingRequests.map(req => ({
            id: req.id,
            movieName: req.movieName,
            state: req.state,
            createdAt: req.createdAt,
            stateUpdatedAt: req.stateUpdatedAt,
            movie: { movieID: req.movieId, name: req.movieName },
            admin: req.admin,
            organizationName: req.organization,
        }));

        return { success: true, data: pendingRequestsArray};
    }
    catch(err){
        return { success: false , message: err.message };
    }
}


// ANALYTICS

export async function getSystemAnalyticsApi() {
    try {
        const response = await api.get("/admin/v1/system-overview");
        const systemAnalytics = response.data;

        // Mapping based on your provided console log
        const analyticsData = {
            numberOfUsers: systemAnalytics.numberOfUsers,
            numberOfMovies: systemAnalytics.numberOfMovies,
            // Access the nested name immediately for easier UI use
            mostPopularOrgName: systemAnalytics.mostPopularOrganization?.name || "N/A",
            // Store IDs to generate links
            mostLikedMovieId: systemAnalytics.mostLikedMovie,
            mostRatedMovieId: systemAnalytics.mostRatedMovie
        };

        return { success: true, data: analyticsData };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function addAdminApi({ name, email, password }) {
    try {
        await api.post("/admin/v1/admins", { name, email, password });
        return { success: true };
    }catch(err){
        return { success: false , message: err.message };
    }
}


// Admin Profile
export async function getAdminProfileApi() {
    try {
        const res = await api.get("/admin/v1/profile");

        return { success: true, data: res.data };
    }catch (err){

        return { success: false , message: err.message };
    }
}

export async function updateNameApi({name}){
    try {
        const res = await api.put("/admin/v1/name", {name});

        return { success: true, data: res.data };
    }catch (err){

        return { success: false , message: err.message };
    }
}

export async function updatePasswordApi({oldPassword, newPassword}){
    try {
        const res = await api.put("/auth/v1/password", {oldPassword, newPassword});

        return { success: true, data: res.data };
    }catch(err){
        const serverMessage = err?.response?.data?.message ?? err?.response?.data ?? err?.message;
        return { success: false , message: serverMessage };
    }
}
