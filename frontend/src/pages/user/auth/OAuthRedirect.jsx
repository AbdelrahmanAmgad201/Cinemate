import { useEffect, useContext, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { AuthContext } from '../../../context/AuthContext.jsx';
import { jwtDecode } from 'jwt-decode';
import { JWT, PATHS } from '../../../constants/constants.jsx';
import api from '../../../api/api-client.js';
import LoadingFallback from '../../../components/LoadingFallback.jsx';

const OAuthRedirect = () => {
    const navigate = useNavigate();
    const { setUser } = useContext(AuthContext);
    const [searchParams] = useSearchParams();
    const exchanged = useRef(false);

    useEffect(() => {
        const code = searchParams.get('code');

        if (!code) {
            navigate(PATHS.ROOT);
            return;
        }

        // The exchange code is single-use; guard against React re-invoking this effect.
        if (exchanged.current) return;
        exchanged.current = true;

        api.post('/auth/v1/oauth-token', { code })
            .then(({ data }) => {
                const token = data.accessToken;
                sessionStorage.setItem(JWT.STORAGE_NAME, token);

                const userData = jwtDecode(token); // returns { id, email, role, iat }

                setUser({
                    id: userData.id,
                    email: userData.email,
                    role: userData.role.replace('ROLE_', ''),
                    profileComplete: userData.profileComplete,
                });

                if (userData.profileComplete === false) {
                    navigate(PATHS.PROFILE_COMPLETION);
                } else {
                    navigate(PATHS.HOME);
                }
            })
            .catch(() => {
                navigate(PATHS.ROOT);
            });
    }, [searchParams, setUser, navigate]);

    return <LoadingFallback fullScreen />;
};

export default OAuthRedirect;
