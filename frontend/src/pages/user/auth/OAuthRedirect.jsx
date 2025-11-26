import { useEffect, useContext } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { AuthContext } from '../../../context/AuthContext.jsx';
import { jwtDecode } from 'jwt-decode';

const OAuthRedirect = () => {
  const navigate = useNavigate();
  const { setUser } = useContext(AuthContext);
  const [searchParams] = useSearchParams();

  useEffect(() =>{
    const token = searchParams.get('token');
    const error = searchParams.get('error');

    if(token){
        try{
            localStorage.setItem('token', token);

            const userData = jwtDecode(token);

            console.log("OAuth sign-in successful");

            navigate('/home-page');
        }
        catch(err){
            console.log("Error decoding token:", err);
            navigate('/');
        }
    }
    else if(error){
        console.log("OAuth sign-in error:", error);
        navigate('/');
    }
    else{
        console.log("No token or error found in URL");
        navigate('/');
    }
  }, [searchParams, setUser, navigate]);
  
  return null;
};
export default OAuthRedirect;