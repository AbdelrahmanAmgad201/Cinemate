import SignUp from '../../auth/SignUp.jsx';
import { PATHS } from '../../../constants/constants.jsx';

const OrgSignUp = () => {
    return (
        <SignUp
            role="Organization"
            show={false}
            link={PATHS.ORGANIZATION.SIGN_IN}
        />
    );
};

export default OrgSignUp;
