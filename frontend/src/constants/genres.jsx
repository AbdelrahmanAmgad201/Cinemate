// Mirrors the backend Genre enum (org.example.backend.movie.Genre) exactly —
// the `key` is sent to the API/used in routes, `label` is what's shown to users.
export const GENRES = [
    { key: 'MYSTERY', label: 'Mystery', color: '#6C5CE7' },
    { key: 'COMEDY', label: 'Comedy', color: '#F2B84B' },
    { key: 'ANIMATION', label: 'Animation', color: '#3DD68C' },
    { key: 'DOCUMENTARY', label: 'Documentary', color: '#5C8DE7' },
    { key: 'ROMANCE', label: 'Romance', color: '#F0679F' },
    { key: 'THRILLER', label: 'Thriller', color: '#E75C5C' },
    { key: 'SCIFI', label: 'Sci-Fi', color: '#36A1F3' },
    { key: 'HORROR', label: 'Horror', color: '#8C3A3A' },
    { key: 'DRAMA', label: 'Drama', color: '#B08968' },
    { key: 'ACTION', label: 'Action', color: '#F0554F' },
];

export function getGenreLabel(key) {
    return GENRES.find((g) => g.key === key?.toUpperCase())?.label || key;
}
