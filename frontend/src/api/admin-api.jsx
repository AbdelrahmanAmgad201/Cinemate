import api from './api-client.jsx';

export async function getRequestsHistoryApi() {
    try{
        const response = await api.post("/admin/v1/find-admin-requests");
        const rawRequestsHistory = response.data;
        console.log(rawRequestsHistory);
        const RequestsHistoryMapped = rawRequestsHistory.map(req => ({
            id: req.id,
            movieName: req.movieName,
            state: req.state,
            createdAt: req.createdAt,
            stateUpdatedAt: req.stateUpdatedAt,
            // movie: req.movie,
            movie: { movieID: req.movieId, name: req.movieName},
            admin: req.admin,
            organizationName: req.organization,
        }));

        // const requests = response.data;
        // console.log(requests);


        return { success: true, data: RequestsHistoryMapped};
    }
    catch(err){
        // console.log(err);
        return { success: false , message: err.message };
    }
}

export async function declineRequestApi({requestId}) {
    try{
        const response = await api.post("/admin/v1/decline-request", null, {
            params: { requestId },
        });

        const data = response.data;
        // console.log(data);


        return { success: true, data: data};
    }
    catch(err){
        // console.log(err);
        return { success: false , message: err.message };
    }
}

export async function acceptRequestApi({requestId}) {
    try{
        const response = await api.post("/admin/v1/accept-request", null, {
            params: { requestId },
        });

        const data = response.data;
        // console.log(data);


        return { success: true, data: data};
    }
    catch(err){
        // console.log(err);
        return { success: false , message: err.message };
    }
}

export async function getPendingRequestsApi() {

    try{
        const response = await api.post("/admin/v1/get-pending-requests");
        console.log(response.data);
        const pendingRequests = response.data;
        const pendingRequestsArray = pendingRequests.map(req => ({
            id: req.id,
            movieName: req.movieName,
            state: req.state,
            createdAt: req.createdAt,
            stateUpdatedAt: req.stateUpdatedAt,
            // movie: req.movie,
            admin: req.admin,
            organizationName: req.organization,
        }));

        // console.log(pendingRequestsArray);


        return { success: true, data: pendingRequestsArray};
    }
    catch(err){
        // console.log(err);
        return { success: false , message: err.message };
    }
}


// ANALYTICS

export async function getSystemAnalyticsApi() {
    try {
        const response = await api.post("/admin/v1/get-system-overview");
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
        // console.log(err);
        return { success: false, message: err.message };
    }
}

export async function getMovieAnalyticsApi({ movieId }) {

    try{
        const response = await api.post("/admin/v1/get-specific-movie-overview", null,  {
            params: { movieId },
        });
        const movieAnalytics = response.data;
        console.log(movieAnalytics);

        // private Long numberOfUsers;
        // private Long numberOfMovies;
        // private Organization mostPopularOrganization;
        // private Long mostLikedMovie;
        // private Long mostRatedMovie;

        const analyticsData ={
            numberOfUsers: movieAnalytics.numberOfUsers,
            numberOfMovies: movieAnalytics.numberOfMovies,
            mostPopularOrganization: movieAnalytics.mostPopularOrganization, // could map .name if you want
            mostLikedMovie: movieAnalytics.mostLikedMovie,
            mostRatedMovie: movieAnalytics.mostRatedMovie
        };

        return { success: true, data: analyticsData};
    }
    catch(err){
        // console.log(err);
        return { success: false , message: err.message };
    }
}

export async function addAdminApi({ name, email, password }) {

    try {
        const res = await api.post("/admin/v1/add-admin", { name, email, password });
        console.log(res);

        return { success: true };

    }catch(err){

        return { success: false , message: err.message };
    }


}


// Admin Profile
export async function getAdminProfileApi() {
    try {
        const res = await api.get("/admin/v1/get-admin-profile");

        return { success: true, data: res.data };
    }catch (err){

        return { success: false , message: err.message };
    }
}

export async function updateNameApi({name}){
    try {
        const res = await api.put("/admin/v1/update-name", {name});

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

        return { success: false , message: err.message };
    }
}