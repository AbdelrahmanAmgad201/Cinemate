import api from "./apiClient";

export const fetchMoviesOverview = async () => {
    try {
        const response = await api.post("/organization/v1/movies-overview");
        return response.data;
    } catch (error) {
        console.error("Failed to fetch movies overview:", error);
        throw error;
    }
};

export const fetchOrgRequests = async () => {
    try {
        const response = await api.post("/organization/v1/get-all-organization-requests");
        return response.data;
    } catch (error) {
        console.error("Failed to fetch organization requests:", error);
        throw error;
    }
};

export const fetchRequestsOverview = async () => {
    try {
        const response = await api.post("/organization/v1/get-requests-over-view");
        return response.data;
    } catch (error) {
        console.error("Failed to fetch requests overview:", error);
        throw error;
    }
};

const fetchOrgAnalytics = async () => {
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
        console.error("Failed to fetch organization analytics:", error);
        throw error;
    }
};

export default fetchOrgAnalytics;
