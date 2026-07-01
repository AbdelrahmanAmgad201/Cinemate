// Initiates the Google OAuth2 flow by redirecting the browser to the backend's OAuth endpoint.
// The backend URL is read from VITE_API_BASE_URL so this works in all environments.
const oauthSignIn = () => {
    const backendUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
    window.location.href = `${backendUrl}/oauth2/authorize/google`;
};

export default oauthSignIn;