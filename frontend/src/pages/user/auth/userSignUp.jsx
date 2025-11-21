import SignUp from "../../auth/signUp.jsx";
import { useNavigate } from 'react-router-dom';

const UserSignUp = () => {
    const navigate = useNavigate();

    return (
        <SignUp/>
    );
}

export default UserSignUp;