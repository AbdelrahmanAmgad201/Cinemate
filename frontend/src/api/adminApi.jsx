import api from './apiClient.jsx';

export async function getRequestsHistoryApi() {
    try{
        const response = await api.post("/admin/v1/find-admin-requests");
        const rawRequestsHistory = response.data;
        const RequestsHistoryMapped = rawRequestsHistory.map(req => ({
            id: req.id,
            movieName: req.movieName,
            state: req.state,
            createdAt: req.createdAt,
            stateUpdatedAt: req.stateUpdatedAt,
            movie: req.movie,
            admin: req.admin,
            organization: req.organization,
        }));

        // const requests = response.data;
        // console.log(requests);


        return { success: true, data: RequestsHistoryMapped};
    }
    catch(err){
        console.log(err);
        return { success: false , message: err.response?.data?.error || err.message  };
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
        console.log(err);
        return { success: false , message: err.response?.data?.error || err.message  };
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
        console.log(err);
        return { success: false , message: err.response?.data?.error || err.message  };
    }
}

export async function getPendingRequestsApi() {
    console.log("getPendingRequestsApi");
    try{
        const response = await api.post("/admin/v1/get-pending-requests");

        const pendingRequests = response.data;
        const pendingRequestsArray = pendingRequests.map(req => ({
            id: req.id,
            movieName: req.movieName,
            state: req.state,
            createdAt: req.createdAt,
            stateUpdatedAt: req.stateUpdatedAt,
            movie: req.movie,
            admin: req.admin,
            organization: req.organization,
        }));

        // console.log(pendingRequestsArray);


        return { success: true, data: pendingRequestsArray};
    }
    catch(err){
        console.log(err);
        return { success: false , message: err.response?.data?.error || err.message  };
    }
}