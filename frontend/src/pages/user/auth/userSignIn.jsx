import SignIn from "../../auth/signIn.jsx";
import { useNavigate } from 'react-router-dom';

const UserSignIn = () => {
    const navigate = useNavigate();

    return (
        <SignIn/>
    );
}

export default UserSignIn;