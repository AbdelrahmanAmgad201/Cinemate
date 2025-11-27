import {Link} from "react-router-dom";

import "./style/navBar.css"

export default function SiteAnalytics() {
    return (
        <div>
            <div className="navigationBar">
                <Link to="/review-movies"><h1>Review Movies</h1></Link>
                <Link to="/admin-site-analytics"><h1>Site Movies and Analytics</h1></Link>
            </div>
        </div>
    )
}