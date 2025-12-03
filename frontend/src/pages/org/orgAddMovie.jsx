import '../auth/style/signUp.css';
import './style/addMovie.css';

import addMovieApi from '../../api/addMovieApi.jsx';
import {useContext, useState} from 'react';
import {Link} from 'react-router-dom';
import ProfileAvatar from "../../components/profileAvatar.jsx";

import {AuthContext} from "../../context/authContext.jsx";
import {MAX_LENGTHS, PATHS} from "../../constants/constants.jsx";
import {ToastContext} from "../../context/ToastContext.jsx";

const OrgAddMovie = () => {

     const [movieName, setMovieName] = useState("");
     const [movieURL, setMovieURL] = useState("");
     const [thumbnailURL, setThumbnailURL] = useState("");
     const [trailerURL, setTrailerURL] = useState("");
     // const [movieDuration, setMovieDuration] = useState("");
     const [durationHours, setDurationHours] = useState("");
     const [durationMinutes, setDurationMinutes] = useState("");
     const [releaseDate, setReleaseDate] = useState("");
     const [genre, setGenre] = useState("");
     const [movieDescription, setMovieDescription] = useState("");

     const {showToast} = useContext(ToastContext);

     const handleSubmit = async (e) =>{
             e.preventDefault();

             const movieData = {
                 name: movieName,
                 description: movieDescription,
                 movieUrl: movieURL,
                 thumbnailUrl: thumbnailURL,
                 trailerUrl: trailerURL,
                 duration: (parseInt(durationHours || 0) * 60) + parseInt(durationMinutes || 0),
                 genre: genre,
                 releaseDate: releaseDate
                 };

             const result = await addMovieApi(movieData);

             if (result.success) {
                 showToast("Success", "Movie added successfully!", "success");
             } else {
                 showToast("Failed to add movie", result.message, "error");
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
                 <Link to = {PATHS.ORGANIZATION.SUBMIT_REQUEST}><h1>Add Movie</h1></Link>
                 <Link to = {PATHS.ORGANIZATION.MOVIES_ANALYTICS}><h1>My Movies and Analytics</h1></Link>
                 <ProfileAvatar menuItems={avatarMenuItems} />
             </div>
             <div className = "signup-container addMovie-container">
                 <form onSubmit={handleSubmit}>
                     <div className = "name">
                         <div className = "input-elem">
                             <label htmlFor = "movieName">Movie Name</label>
                             <input type = "text" id = "movieName" name = "movieName" maxLength={MAX_LENGTHS.INPUT} required  placeHolder = "Enter movie name" onChange={(e) => {setMovieName(e.target.value)}}/>
                         </div>
                         <div className = "input-elem">
                             <label htmlFor = "movieURL">Movie</label>
                             <input type = "text" id = "movieURL" name = "movieURL" maxLength={MAX_LENGTHS.INPUT} required placeHolder = "Enter wistia id" onChange={(e) => {setMovieURL(e.target.value)}}/>
                         </div>
                         <div className = "input-elem">
                             <label htmlFor = "thumbnailURL">Thumbnail URL</label>
                             <input type = "text" id = "thumbnailURL" name = "thumbnailURL" maxLength={MAX_LENGTHS.URL} required placeHolder = "Enter movie thumbnail URL" onChange={(e) => {setThumbnailURL(e.target.value)}}/>
                         </div>
                     </div>
                         <div className = "name">
                             <div className = "input-elem">
                                 <label htmlFor = "trailerURL">Trailer</label>
                                 <input type = "text" id = "trailerURL" name = "trailerURL" maxLength={MAX_LENGTHS.URL} required placeHolder = "Enter wistia id" onChange={(e) => {setTrailerURL(e.target.value)}}/>
                             </div>
                             <div className = "input-elem duration-group">
                                 <label>Movie Duration</label>
                                 <div className="duration-inputs">
                                     <div className="duration-field">
                                         <label htmlFor="durationHours">Hours</label>
                                         <input
                                             type="number"
                                             id="durationHours"
                                             name="durationHours"
                                             value={durationHours}
                                             onChange={(e) => {
                                                 let val = e.target.value.replace(/[^0-9]/g, ''); // remove non-digit characters
                                                 if (val === "") {
                                                     setDurationHours(""); // allow empty input
                                                     return;
                                                 }

                                                 // Limit number of digits
                                                 if (val.length <= MAX_LENGTHS.MOVIE_HOURS) {
                                                     setDurationHours(val);
                                                 }
                                             }}
                                             min="0"
                                             placeholder="0"
                                             required
                                         />
                                     </div>
                                     <div className="duration-field">
                                         <label htmlFor="durationMinutes">Minutes</label>
                                         <input
                                             type="number"
                                             id="durationMinutes"
                                             name="durationMinutes"
                                             value={durationMinutes}
                                             onChange={(e) => {
                                                 let val = e.target.value.replace(/[^0-9]/g, '').slice(0, 2);
                                                 const numVal = parseInt(val, 10);
                                                 if (val === "" || numVal <= 59) {
                                                     setDurationMinutes(val);
                                                 }
                                             }}
                                             min="0"
                                             max="59"
                                             maxLength="2"
                                             placeholder="00"
                                             required
                                         />

                                     </div>
                                 </div>
                             </div>
                             {/*<div className="date-gender">*/}
                             {/*    <div className="input-elem">*/}
                             {/*        <label htmlFor="releaseDate">Release Date</label><br />*/}
                             {/*        <div className="icon-input">*/}
                             {/*            <CiCalendar />*/}
                             {/*            <input type="date" id="releaseDate" name="releaseDate" required style={{width: "200px"}} onChange={(e) => {setReleaseDate(e.target.value)}} />*/}
                             {/*        </div>*/}
                             {/*    </div>*/}
                             {/*</div>*/}
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
                         <textarea id = "movieDescription" required maxLength={MAX_LENGTHS.TEXTAREA} placeHolder = "Enter movie description" onChange={(e) => {setMovieDescription(e.target.value)}}/>
                     </div>
                         <button type="submit">Request Add Movie</button><br/>
                         </form>
                 </div>
             </div>
         );
    };

export default OrgAddMovie;