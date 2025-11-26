import '../../auth/style/signUp.css';
import './style/addMovie.css';
import {Link} from 'react-router-dom';

const OrgMoviesAndAnalytics = () => {
    return(
        <div>
            <div className = "navigationBar">
                <Link to = "/"><h1>Home Page</h1></Link>
                <Link to = "/org-add-movie"><h1>Add Movie</h1></Link>
                <Link to = "/org-movies-and-analytics"><h1>My Movies and Analytics</h1></Link>
            </div>
        </div>
    );
};

export default OrgMoviesAndAnalytics;