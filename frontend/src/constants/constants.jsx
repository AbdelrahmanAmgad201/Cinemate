
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