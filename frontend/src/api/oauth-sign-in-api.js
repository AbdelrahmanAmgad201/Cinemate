import { BACKEND_URL } from "../constants/constants.jsx";

// Initiates the Google OAuth2 flow by redirecting the browser to the backend's OAuth endpoint.
const oauthSignIn = () => {
    window.location.href = `${BACKEND_URL.BASE_URL}/oauth2/authorize/google`;
};

export default oauthSignIn;