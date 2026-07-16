// In-memory access-token store (FE-01). Deliberately not sessionStorage/localStorage:
// storage APIs are readable by any injected script, while a module-level variable can
// only be reached by an attacker who hooks the app's own execution. The trade-off is
// it resets on a full page reload — AuthContext already covers that by silently
// calling /refresh against the httpOnly refresh cookie on bootstrap.
let accessToken = null;

export function getAccessToken() {
    return accessToken;
}

export function setAccessToken(token) {
    accessToken = token;
}

export function clearAccessToken() {
    accessToken = null;
}
