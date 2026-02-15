export const BACKEND_URL = {
    BASE_URL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
    // TODO: Add this to the env
    WATCH_PARTY_BASE_URL: import.meta.env.VITE_API_WATCH_PARTY_BASE_URL || "http://localhost:8081",

}
export const MAX_LENGTHS = {
    // These are the max lengths in chars
    TEXTAREA: 2000, //65000 in DB
    INPUT: 100, //255 in DB
    URL: 255,
    MOVIE_HOURS: 2,
};

export const MIN_LENGTHS = {
    // These are the min lengths in chars
    PASSWORD: 8,
    INPUT: 1,
};

export const MAX_VALUES = {
    // These are the max values
    BIRTHYEARS: 110,
};

export const PAGE_SIZE = {
    FORUM : 5,
}

export const ROLES = {
    USER: "USER",
    ORGANIZATION: "ORGANIZATION",
    ADMIN: "ADMIN",
    
    WATCH_PARTY_HOST: "WATCH_PARTY_HOST",
    WATCH_PARTY_GUEST: "WATCH_PARTY_GUEST",
}

export const PATHS = {
    ROOT: "/",
    HOME: "/home-page",
    EMAIL_VERIFICATION: "/email-verification",
    PROFILE_COMPLETION: "/profile-completion",

    GOOGLE_AUTH : {
        REDIRECT: "/oauth2/redirect",
    },

    USER : {
        SIGN_IN: "/user/sign-in", // Old was "/"
        SIGN_UP: "/user/sign-up", // Old was "/user-sign-up"
        PROFILE: (id = ":userId") => `/user/${id}`,
        FOLLOWERS: (id = ":userId") => `/user/${id}/followers`,
        FOLLOWING: (id = ":userId") => `/user/${id}/following`,
    },

    ADMIN : {
        SIGN_IN: "/admin/sign-in", // Old was "/admin-sign-in"
        REVIEW_REQUESTS: "/admin/review-requests", // Old was "/review-movies"
        SITE_ANALYTICS: "/admin/site-analytics", // Old was "/admin-site-analytics"
        ADD_ADMIN: "/admin/add-admin",
        PROFILE: (id = ":userId") => `/admin/${id}`,
    },

    ORGANIZATION : {
        SIGN_IN: "/organization/sign-in", // Old was "/org-sign-in"
        SIGN_UP: "/organization/sign-up", // Old was "/org-sign-up"
        SUBMIT_REQUEST: "/organization/submit-request", // Old was "/org-add-movie"
        MOVIES_ANALYTICS: "/organization/movies-analytics", // Old was "/org-movies-and-analytics"
        PROFILE: (id = ":orgId") => `/organization/${id}`,
    },

    MOVIE: {
        DETAILS: (id = ":movieId") => `/movie/${id}`, // "/movie/:movieId"
        // Usage -> navigate(PATHS.MOVIE.DETAILS(movieId))
        BROWSE: "/browse",
        GENRE: (title = ":title") => `/genre/${title}`, // "/genre/:title"
        WATCH: "/watch",
        WATCH_PARTY: (roomId = ":roomId") => `/watch-party/${roomId}`,

    },

    POST: {
        FULLPAGE: (id = ":postId") => `/post/${id}`,
        THREAD: (id = ":commentId") => `/post/thread/${id}`,
    },

    FORUM : {
        EXPLORE: '/forums-explore',
        PAGE : (id = ":forumId") => `/forum/${id}`, // May change to name if it's unique
    },

    MOD : {
        PAGE: (id = ":forumId") => `/mod/${id}`,
    }
    ,
    
}

export const JWT = {
    STORAGE_NAME : "CINEMATE_JWT_TOKEN",
}

export const SESSION_STORAGE = {
    USER_SIDE_BAR_NAME : "CINEMATE_USER_SIDE_BAR",
    ACTIVE_PARTY_KEY: (userId) => `CINEMATE_PARTY_SESSION_USER_${userId}`,
}

export const WatchPartyEventType = {
    // These match the backend
    PLAY: "PLAY",
    PAUSE: "PAUSE",
    SEEK: "SEEK",
    // Client requests host to send current time so the requester can sync
    SYNC_REQUEST: "SYNC_REQUEST",

    CHAT: "CHAT",
    USER_JOINED: "USER_JOINED",
    USER_LEFT: "USER_LEFT",
    PARTY_DELETED: "PARTY_DELETED",
}

export const WATCH_PARTY = {
    // These for uses in frontend
    END: "END",
    SYNC_TIME: "SYNC_TIME",
    LAG_THRESHOLD: 2, // seconds
}