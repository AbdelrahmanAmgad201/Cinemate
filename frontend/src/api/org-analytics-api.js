import api from "./api-client.js";

export const fetchMoviesOverviewApi = async () => {
    try {
        const response = await api.get("/organization/v1/movies-overview");
        return response.data;
    } catch (error) {
        return { success: false , message: error.message };
    }
};

export const fetchOrgRequestsApi = async () => {
    try {
        const response = await api.get("/organization/v1/requests");
        return response.data;
    } catch (error) {
        return { success: false , message: error.message };
    }
};

export async function fetchRequestsOverviewApi (){
    try {
        const response = await api.get("/organization/v1/requests-overview");
        return response.data;
    } catch (error) {
        return { success: false , message: error.message };
    }
};

export async function fetchOrgAnalyticsApi (){
    try {
        const [moviesOverview, requestsOverview] = await Promise.all([
            fetchMoviesOverviewApi(),
            fetchRequestsOverviewApi()
        ]);
        return {
            moviesOverview,
            requestsOverview
        };
    } catch (error) {
        return { success: false , message: error.message };
    }
};

export async function fetchOrgProfileApi() {
    try {
        const response = await api.get("/organization/v1/personal-data");
        return {success: true, response: response.data};
    } catch (error) {
        return { success: false , message: error.message };
    }
}

export async function updateOrgProfileApi(data) {
    try {
        const response = await api.put("/organization/v1/profile", data);
        return {success: true, response: response.data};
    } catch (error) {
        return { success: false , message: error.message };
    }
}

export async function fetchOrgMoviesApi(page = 0, size = 20) {
    try {
        const response = await api.get("/organization/v1/my-movies", {
            params: {
                page: page,
                size: size
            }
        });
        return {success: true, response: response.data};
    } catch (error) {
        return { success: false , message: error.message };
    }
}

export async function updateOrgPasswordApi(oldPassword, newPassword) {
    try {
        const response = await api.put("/auth/v1/password", {
            oldPassword: oldPassword,
            newPassword: newPassword
        });
        return {success: true, response: response.data};
    } catch (error) {
        return { success: false , message: error.message };
    }
}
