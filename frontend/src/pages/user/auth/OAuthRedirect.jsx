import { useEffect, useContext } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { AuthContext } from '../../../context/AuthContext.jsx';
import { jwtDecode } from 'jwt-decode';
import {JWT, PATHS} from "../../../constants/constants.jsx";

const OAuthRedirect = () => {
  const navigate = useNavigate();
  const { setUser } = useContext(AuthContext);
  const [searchParams] = useSearchParams();

  useEffect(() =>{
    const token = searchParams.get('token');

    if(token){
        try{
            sessionStorage.setItem(JWT.STORAGE_NAME, token);

            const userData = jwtDecode(token); // returns { id, email, role, iat }

            setUser({
                id: userData.id,
                email: userData.email,
                role: userData.role.replace("ROLE_", ""),
                profileComplete: userData.profileComplete,
            });



            if (userData.profileComplete === false) {
                navigate(PATHS.PROFILE_COMPLETION);
            } 
            else {
                navigate(PATHS.HOME);
            }
        }
        catch(err){
            console.log("Error decoding token:", err);
            navigate(PATHS.ROOT);
        }
    }
    else{
        console.log("No token found in URL");
        navigate(PATHS.ROOT);
    }
  }, [searchParams, setUser, navigate]);
  
  return null;
};
export default OAuthRedirect;