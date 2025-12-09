import "./style/AddAdmin.css"
import "./style/NavBar.css";

import {useContext, useState} from "react";
import {AuthContext} from "../../context/AuthContext.jsx";
import {ToastContext} from "../../context/ToastContext.jsx";
import {Link } from "react-router-dom";


import ProfileAvatar from "../../components/ProfileAvatar.jsx";
import {LuEye, LuEyeOff} from "react-icons/lu";

import {addAdminApi} from "../../api/admin-api.jsx";

import Swal from "sweetalert2";
import {MAX_LENGTHS, PATHS, ROLES} from "../../constants/constants.jsx";

export default function AddAdmin() {

    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);

    const { signOut } = useContext(AuthContext);
    const { showToast } = useContext(ToastContext);

    const avatarMenuItems = [
        { label: "Sign Out", onClick: signOut },
    ];

    const handleSubmit = async (e) => {
        e.preventDefault();

        const result = await Swal.fire({
            title: "Confirm Admin Creation?",
            text: `Are you sure you want to create the admin account for ${name}?`,
            icon: "question",
            showCancelButton: true,
            confirmButtonText: "Yes, Create Admin",
            cancelButtonText: "Cancel",
            confirmButtonColor: '#36A1F3',
        });

        if (!result.isConfirmed) return;

        const userData = {
            name: name,
            email: email,
            password: password,
        }

        const res = await addAdminApi(userData)
        if (res.success === true){
            showToast("Sign up success.", "New admin added successfully.", "success")
        }
        else{
            showToast("Sign up failed.", res.message || "Sign up failed. Please try again.", "error")
        }
    }

    return (
        <div className="add-admin-page">
            <div className="navigationBar">
                <Link to={PATHS.ADMIN.REVIEW_REQUESTS} ><h1>Review Movies</h1></Link>
                <Link to={PATHS.ADMIN.SITE_ANALYTICS} ><h1>Site Movies and Analytics</h1></Link>
                <Link to={PATHS.ADMIN.ADD_ADMIN} ><h1>Add New Admin</h1></Link>
                <ProfileAvatar menuItems={avatarMenuItems} />
            </div>

            <form onSubmit={handleSubmit}>

                <div className="input-field name">
                    <label htmlFor="name">Name</label>
                    <input type="text"
                           id="name"
                           name="name"
                           value={name}
                           maxLength={MAX_LENGTHS.INPUT}
                           placeholder="Enter admin's name"
                           required
                           onChange={e => setName(e.target.value)}
                    />
                </div>

                <div className="input-field email">
                    <label htmlFor="email">Email</label>
                    <input type="email"
                           id="email"
                           name="email"
                           value={email}
                           maxLength={MAX_LENGTHS.INPUT}
                           placeholder="Enter admin's email"
                           required
                           onChange={e => setEmail(e.target.value)}
                    />
                </div>

                <div className="input-field password">
                    <label htmlFor="password">Password</label>
                    <div className="password-wrapper">
                        <input type={showPassword ? "text" : "password"}
                               id="password"
                               name="password"
                               minLength={8}
                               maxLength={100}
                               value={password}
                               placeholder="Enter admin's password"
                               required
                               onChange={(e) => {
                                   setPassword(e.target.value)
                               }}/>
                        <span className="password-toggle-icon"
                              onClick={() => setShowPassword(!showPassword)}>
                        {showPassword ? <LuEye/> : <LuEyeOff/>}
                        </span>
                    </div>
                </div>

                <button type="submit">Add Admin</button>

            </form>
        </div>
    );
}