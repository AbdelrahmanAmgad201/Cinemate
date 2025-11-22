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
            setUser({
                id: userData.id,
                email: userData.email,
                role: userData.role,
            });

            console.log("OAuth sign-in successful");

            navigate('/homePage');
        }
        catch(err){
            console.log("Error decoding token:", err);
            navigate('/signIn');
        }
    }
    else if(error){
        console.log("OAuth sign-in error:", error);
        navigate('/signIn');
    }
    else{
        console.log("No token or error found in URL");
        navigate('/signIn');
    }
  }, [searchParams, setUser, navigate]);
  
  return null;
};
export default OAuthRedirect;