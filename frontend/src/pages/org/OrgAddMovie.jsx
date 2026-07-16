import './style/addMovie.css';
import { useContext, useState } from 'react';
import NavBar from '../../components/OrgAdminNavBar.jsx';
import { Film, Image, Clock } from 'lucide-react';

import addMovieApi from '../../api/add-movie-api.js';

import { MAX_LENGTHS } from '../../constants/constants.jsx';
import { GENRES } from '../../constants/genres.jsx';
import { ToastContext } from '../../context/ToastContext.jsx';
import Card from '../../components/ui/Card.jsx';
import Input from '../../components/ui/Input.jsx';
import Textarea from '../../components/ui/Textarea.jsx';
import Select from '../../components/ui/Select.jsx';
import Button from '../../components/ui/Button.jsx';

const GENRE_OPTIONS = GENRES.map((g) => ({ value: g.key, label: g.label }));

const OrgAddMovie = () => {
    const [movieName, setMovieName] = useState('');
    const [movieURL, setMovieURL] = useState('');
    const [thumbnailURL, setThumbnailURL] = useState('');
    const [trailerURL, setTrailerURL] = useState('');
    const [durationHours, setDurationHours] = useState('');
    const [durationMinutes, setDurationMinutes] = useState('');
    const [genre, setGenre] = useState('');
    const [movieDescription, setMovieDescription] = useState('');
    const [submitting, setSubmitting] = useState(false);

    const { showToast } = useContext(ToastContext);

    const handleInvalid = (e) => {
        const label = e.target.labels?.[0]?.textContent || e.target.placeholder || 'A required field';
        showToast('Missing information', `${label} is required.`, 'warning');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSubmitting(true);

        const movieData = {
            name: movieName,
            description: movieDescription,
            movieUrl: movieURL,
            thumbnailUrl: thumbnailURL,
            trailerUrl: trailerURL,
            duration: (parseInt(durationHours || 0, 10) * 60) + parseInt(durationMinutes || 0, 10),
            genre,
        };

        const result = await addMovieApi(movieData);

        if (result.success) {
            showToast('Success', 'Movie added successfully!', 'success');
        } else {
            showToast('Failed to add movie', result.message, 'error');
        }
        setSubmitting(false);
    };

    return (
        <div>
            <NavBar />
            <div className="add-movie-container">
                <form onSubmit={handleSubmit} onInvalidCapture={handleInvalid} className="add-movie-form">
                <Card padding="lg">
                    <h1 className="add-movie-title">Submit a movie</h1>

                    <div className="add-movie-row">
                        <Input label="Movie name" maxLength={MAX_LENGTHS.INPUT} required placeholder="Enter movie name" onChange={(e) => setMovieName(e.target.value)} />
                        <Input label="Movie (Wistia ID)" icon={<Film size={16} />} maxLength={MAX_LENGTHS.INPUT} required placeholder="Enter Wistia ID" onChange={(e) => setMovieURL(e.target.value)} />
                    </div>

                    <div className="add-movie-row">
                        <Input label="Thumbnail URL" icon={<Image size={16} />} maxLength={MAX_LENGTHS.URL} required placeholder="Enter thumbnail URL" onChange={(e) => setThumbnailURL(e.target.value)} />
                        <Input label="Trailer (Wistia ID)" icon={<Film size={16} />} maxLength={MAX_LENGTHS.URL} required placeholder="Enter Wistia ID" onChange={(e) => setTrailerURL(e.target.value)} />
                    </div>

                    <div className="add-movie-row">
                        <Select label="Genre" placeholder="Select a genre" options={GENRE_OPTIONS} required value={genre} onChange={(e) => setGenre(e.target.value)} />
                        <div className="duration-field-group">
                            <span className="field__label"><Clock size={14} /> Duration</span>
                            <div className="duration-inputs">
                                <Input
                                    type="number" min="0" placeholder="Hours" required
                                    value={durationHours}
                                    onChange={(e) => setDurationHours(e.target.value.replace(/[^0-9]/g, '').slice(0, MAX_LENGTHS.MOVIE_HOURS))}
                                />
                                <Input
                                    type="number" min="0" max="59" placeholder="Minutes" required
                                    value={durationMinutes}
                                    onChange={(e) => {
                                        const val = e.target.value.replace(/[^0-9]/g, '').slice(0, 2);
                                        if (val === '' || parseInt(val, 10) <= 59) setDurationMinutes(val);
                                    }}
                                />
                            </div>
                        </div>
                    </div>

                    <Textarea
                        label="Movie description"
                        required
                        maxLength={MAX_LENGTHS.TEXTAREA}
                        placeholder="Enter movie description"
                        value={movieDescription}
                        onChange={(e) => setMovieDescription(e.target.value)}
                        rows={5}
                    />

                    <Button type="submit" loading={submitting}>Submit request</Button>
                </Card>
                </form>
            </div>
        </div>
    );
};

export default OrgAddMovie;
