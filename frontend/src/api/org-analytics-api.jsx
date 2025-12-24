import api from "./api-client.jsx";

export const fetchMoviesOverview = async () => {
    try {
        const response = await api.post("/organization/v1/movies-overview");
        return response.data;
    } catch (error) {
        // console.error("Failed to fetch movies overview:", error);
        // throw error;
        return { success: false , message: error.message };

    }
};

export const fetchOrgRequests = async () => {
    try {
        const response = await api.post("/organization/v1/get-all-organization-requests");
        return response.data;
    } catch (error) {
        // console.error("Failed to fetch organization requests:", error);
        // throw error;
        return { success: false , message: error.message };

    }
};

export async function fetchRequestsOverview (){
    try {
        const response = await api.post("/organization/v1/get-requests-over-view");
        return response.data;
    } catch (error) {
        // console.error("Failed to fetch requests overview:", error);
        // throw error;
        return { success: false , message: error.message };

    }
};

export async function fetchOrgAnalytics (){
    try {
        const [moviesOverview, requestsOverview] = await Promise.all([
            fetchMoviesOverview(),
            fetchRequestsOverview()
        ]);
        return {
            moviesOverview,
            requestsOverview
        };
    } catch (error) {
        // console.error("Failed to fetch organization analytics:", error);
        // throw error;
        return { success: false , message: error.message };

    }
};

export async function fetchOrgProfile() {
    try {
        const response = await api.get("/organization/v1/personal-data");
        return {success: true, response: response.data};
    } catch (error) {
        // console.error("Failed to fetch organization profile:", error);
        // throw error;
        return { success: false , message: error.message };

    }
}

export async function updateOrgProfile(data) {
    try {
        const response = await api.post("/organization/v1/set-organization-data", data);
        return {success: true, response: response.data};
    } catch (error) {
        // console.error("Failed to update organization profile:", error);
        // throw error;
        return { success: false , message: error.message };
    }
}

export async function fetchOrgMovies(page = 0, size = 20) {
    try {
        const response = await api.get("/organization/v1/my-movies", {
            params: {
                page: page,
                size: size
            }
        });
        return {success: true, response: response.data};
    } catch (error) {
        // console.error("Failed to fetch organization profile:", error);
        // throw error;
        return { success: false , message: error.message };

    }
}