import "./style/notFoundPage.css"
import { useNavigate } from "react-router-dom";

export default function NotFoundPage() {
    const navigate = useNavigate();

    return (
        <div className="notfound-container">
            <h1>404</h1>
            <h2>Page Not Found</h2>
            <p>The page you are looking for does not exist.</p>
            <button onClick={() => navigate("/home-page")}>Go Home</button>
        </div>
    )
}