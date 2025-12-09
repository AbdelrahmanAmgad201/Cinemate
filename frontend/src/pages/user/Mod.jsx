import {useContext} from "react";
import {AuthContext} from "../../context/AuthContext.jsx";
import {useParams} from "react-router-dom";


export default function Mod() {

    const { user } = useContext(AuthContext)
    const { forumId } = useParams();

    return (
        <h1>Mod</h1>
    );
}