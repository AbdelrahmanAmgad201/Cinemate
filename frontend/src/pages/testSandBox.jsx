import React, { useState } from 'react';
import MoviesDetailsApi from '../api/movies-details-api.jsx';

export default function TestSandBox() {
    const [newReleases, setNewReleases] = useState([]);
    const [topRated, setTopRated] = useState([]);
    const [loading, setLoading] = useState(false);

    const newReleasesRequest = {
        name: null,
        genre: null,
        sortBy: "releaseDate",
        sortDirection: "desc",
        page: 0,
        pageSize: 6
    };

    const topRatedRequest = {
        name: null,
        genre: null,
        sortBy: "rating",
        sortDirection: "desc",
        page: 0,
        pageSize: 6
    };

    const fetchNewReleases = async () => {
        setLoading(true);
        const result = await MoviesDetailsApi(newReleasesRequest);
        if (result.success) {
            console.log("New Releases:", result.movies);
            setNewReleases(result.movies);
        }
        setLoading(false);
    };

    const fetchTopRated = async () => {
        setLoading(true);
        const result = await MoviesDetailsApi(topRatedRequest);
        if (result.success) {
            console.log("Top Rated:", result.movies);
            setTopRated(result.movies);
        }
        setLoading(false);
    };

    return (
        <div>
            <h1>Test Search API</h1>
            <button onClick={fetchNewReleases}>Fetch New Releases</button>
            <button onClick={fetchTopRated}>Fetch Top Rated</button>

            {loading && <p>Loading...</p>}

            <h2>New Releases</h2>
            <pre>{JSON.stringify(newReleases, null, 2)}</pre>

            <h2>Top Rated</h2>
            <pre>{JSON.stringify(topRated, null, 2)}</pre>
        </div>
    );
}
