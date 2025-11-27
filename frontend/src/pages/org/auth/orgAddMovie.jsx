import '../../auth/style/signUp.css';
import './style/addMovie.css';
import addMovieApi from '../../../api/addMovieApi.jsx';
import {useContext, useState} from 'react';
import { CiCalendar } from "react-icons/ci";
import { useNavigate } from 'react-router-dom';
import {Link} from 'react-router-dom';
import ProfileAvatar from "../../../components/profileAvatar.jsx";
import {AuthContext} from "../../../context/authContext.jsx";

const OrgAddMovie = () => {

     const [movieName, setMovieName] = useState("");
     const [movieURL, setMovieURL] = useState("");
     const [thumbnailURL, setThumbnailURL] = useState("");
     const [trailerURL, setTrailerURL] = useState("");
     const [movieDuration, setMovieDuration] = useState("");
     const [releaseDate, setReleaseDate] = useState("");
     const [genre, setGenre] = useState("");
     const [movieDescription, setMovieDescription] = useState("");

     const handleSubmit = async (e) =>{
             e.preventDefault();

             const movieData = {
                 name: movieName,
                 description: movieDescription,
                 movieUrl: movieURL,
                 thumbnailUrl: thumbnailURL,
                 trailerUrl: trailerURL,
                 duration: parseInt(movieDuration),
                 genre: genre,
                 releaseDate: releaseDate
                 };

             const result = await addMovieApi(movieData);

             if (result.success) {
                 alert("Movie added successfully!");
                 // navigate("/org-analytics");
             } else {
                 alert("Failed to add movie: " + result.message);
             }
         };

    const {signOut} = useContext(AuthContext);
    const avatarMenuItems = [
        // { label: "Profile", onClick: () => console.log("Profile clicked") },
        // { label: "Settings", onClick: () => console.log("Settings clicked") },
        { label: "Sign Out", onClick: signOut },
    ];

     return (
         <div>
             <div className = "navigationBar">
                 {/*<Link to = "/"><h1>Home Page</h1></Link>*/}
                 <Link to = "/org-add-movie"><h1>Add Movie</h1></Link>
                 <Link to = "/org-movies-and-analytics"><h1>My Movies and Analytics</h1></Link>
                 <ProfileAvatar menuItems={avatarMenuItems} />
             </div>
             <div className = "signup-container addMovie-container">
                 <form onSubmit={handleSubmit}>
                     <div className = "name">
                         <div className = "input-elem">
                             <label htmlFor = "movieName">Movie Name</label>
                             <input type = "text" id = "movieName" name = "movieName" required  placeHolder = "Enter movie name" onChange={(e) => {setMovieName(e.target.value)}}/>
                         </div>
                         <div className = "input-elem">
                             <label htmlFor = "movieURL">Movie URL</label>
                             <input type = "text" id = "movieURL" name = "movieURL" required placeHolder = "Enter movie URL" onChange={(e) => {setMovieURL(e.target.value)}}/>
                         </div>
                         <div className = "input-elem">
                             <label htmlFor = "thumbnailURL">Thumbnail URL</label>
                             <input type = "text" id = "thumbnailURL" name = "thumbnailURL" required placeHolder = "Enter movie thumbnail URL" onChange={(e) => {setThumbnailURL(e.target.value)}}/>
                         </div>
                     </div>
                         <div className = "name">
                             <div className = "input-elem">
                                 <label htmlFor = "trailerURL">Trailer URL</label>
                                 <input type = "text" id = "trailerURL" name = "trailerURL" required placeHolder = "Enter movie trailer URL" onChange={(e) => {setTrailerURL(e.target.value)}}/>
                             </div>
                             <div className = "input-elem">
                                 <label htmlFor = "movieDuration">Movie Duration</label>
                                 <input type = "number" id = "movieDuration" name = "movieDuration" required placeHolder = "Enter movie duration (in minutes)" onChange={(e) => {setMovieDuration(e.target.value)}}/>
                             </div>
                             <div className="date-gender">
                                 <div className="input-elem">
                                     <label htmlFor="releaseDate">Release Date</label><br />
                                     <div className="icon-input">
                                         <CiCalendar />
                                         <input type="date" id="releaseDate" name="releaseDate" required style={{width: "200px"}} onChange={(e) => {setReleaseDate(e.target.value)}} />
                                     </div>
                                 </div>
                             </div>
                         </div>
                     <div className="input-elem" style={{borderBottom: "none"}}>
                         <label htmlFor="genre">Genre</label>
                         <div>
                             <div className="gender-options">
                                 <input type = "radio" id = "mystery" name = "genre" value = "MYSTERY" required onChange={(e) => {setGenre(e.target.value)}}/>
                                 <label htmlFor="mystery">MYSTERY</label>
                                 <input type = "radio" id = "comedy" name = "genre" value = "COMEDY" required onChange={(e) => {setGenre(e.target.value)}}/>
                                 <label htmlFor="comedy">COMEDY</label>
                                 <input type = "radio" id = "animation" name = "genre" value = "ANIMATION" required onChange={(e) => {setGenre(e.target.value)}}/>
                                 <label htmlFor="animation">ANIMATION</label>
                                 <input type = "radio" id = "documentary" name = "genre" value = "DOCUMENTARY" required onChange={(e) => {setGenre(e.target.value)}}/>
                                 <label htmlFor="documentary">DOCUMENTARY</label>
                                 <input type = "radio" id = "romance" name = "genre" value = "ROMANCE" required onChange={(e) => {setGenre(e.target.value)}}/>
                                 <label htmlFor="romance">ROMANCE</label>
                             </div>
                             <div className="gender-options">
                                 <input type = "radio" id = "thriller" name = "genre" value = "THRILLER" required onChange={(e) => {setGenre(e.target.value)}}/>
                                 <label htmlFor="thriller">THRILLER</label>
                                 <input type = "radio" id = "scifi" name = "genre" value = "SCIFI" required onChange={(e) => {setGenre(e.target.value)}}/>
                                 <label htmlFor="scifi">SCIFI</label>
                                 <input type = "radio" id = "horror" name = "genre" value = "HORROR" required onChange={(e) => {setGenre(e.target.value)}}/>
                                 <label htmlFor="horror">HORROR</label>
                                 <input type = "radio" id = "drama" name = "genre" value = "DRAMA" required onChange={(e) => {setGenre(e.target.value)}}/>
                                 <label htmlFor="drama">DRAMA</label>
                                 <input type = "radio" id = "action" name = "genre" value = "ACTION" required onChange={(e) => {setGenre(e.target.value)}}/>
                                 <label htmlFor="action">ACTION</label>
                             </div>
                         </div>
                     </div>
                     <div className = "input-elem" style = {{width:"900px"}}>
                         <label htmlFor = "movieDescription">Movie Description</label>
                         <textarea id = "movieDescription" required placeHolder = "Enter movie description" onChange={(e) => {setMovieDescription(e.target.value)}}/>
                     </div>
                         <button type="submit">Request Add Movie</button><br/>
                         </form>
                 </div>
             </div>
         );
    };

export default OrgAddMovie;