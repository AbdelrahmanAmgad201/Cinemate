import React, {useContext, useEffect, useState} from 'react';
import { getReviewsApi, postReviewApi } from "../api/movieApi.jsx";
import {getPendingRequestsApi, acceptRequestApi, declineRequestApi, getRequestsHistoryApi} from "../api/adminApi.jsx";
import {AuthContext} from "../context/authContext.jsx";

export default function TestSandbox() {

    const { user, loading, signIn, signOut, isAuthenticated } = useContext(AuthContext);

    async function processPendingRequests() {
        try {
            // Accept request with ID 1
            const acceptRes = await acceptRequestApi({ requestId: 1 });
            if (acceptRes.success) {
                console.log("Request 1 accepted:", acceptRes.data);
            } else {
                console.error("Failed to accept request 1:", acceptRes.message);
            }

            // Decline request with ID 2
            const declineRes = await declineRequestApi({ requestId: 2 });
            if (declineRes.success) {
                console.log("Request 2 declined:", declineRes.data);
            } else {
                console.error("Failed to decline request 2:", declineRes.message);
            }
        } catch (err) {
            console.error("Error processing requests:", err);
        }
    }

    // Fetch reviews on mount
    useEffect(() => {
        // Wait until auth finishes loading and user exists
        if (loading || !user) return;

        const fetchRequests = async () => {
            try {
                console.log("Logged-in admin:", user);
                const pending = await getPendingRequestsApi({ adminId: user.id });
                const history = await getRequestsHistoryApi({ adminId: user.id });

                console.log("Pending requests:", pending);
                console.log("Requests history:", history);

                await processPendingRequests()
            } catch (err) {
                console.error("Error fetching requests:", err);
            }
        };

        fetchRequests();
    }, [loading, user]); // run again if loading/user changes




    return (
        <div>
            Sandbox
        </div>
    );
}
