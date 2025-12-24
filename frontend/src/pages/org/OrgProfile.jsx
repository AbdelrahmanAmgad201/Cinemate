import { useState, useContext, useEffect } from 'react';
import NavBar from '../../components/OrgAdminNavBar';
import { ToastContext } from '../../context/ToastContext';
import { StatCard } from './OrgMoviesAndAnalytics';
import { fetchOrgProfile, updateOrgProfile } from '../../api/org-analytics-api';
import './style/orgProfile.css';
import LoadingFallback from '../../components/LoadingFallback';
import { GoOrganization } from "react-icons/go";
import { LuEyeOff, LuEye } from "react-icons/lu";
import { FcGoogle } from "react-icons/fc";
import { HiOutlineMail, HiOutlineLockClosed } from "react-icons/hi";
import { AiOutlineUser } from "react-icons/ai";
import { CiCalendar } from "react-icons/ci";
import { MdEdit } from "react-icons/md";
import { BiMessageSquareDetail } from "react-icons/bi";

import { MAX_LENGTHS, MIN_LENGTHS } from '../../constants/constants';

export default function OrgProfile() {

    
    const { showToast } = useContext(ToastContext);

    const [loading, setLoading] = useState(false);
    const [editMode, setEditMode] = useState(false);
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [confirmPassword, setConfirmPassword] = useState("");
    const [editData, setEditData] = useState({
        name: "",
        about: "",
        password: ""
    });
    const [orgData, setOrgData] = useState({
        name: "namePlaceholder",
        about: "aboutPlaceholder",
        email: "emailPlaceholder",
        createdAt: "createdAtPlaceholder",
    });

    useEffect(() => {
        getOrgData();
    }, []);

    const getOrgData = async () => {
        try{
            setLoading(true);
            const result = await fetchOrgProfile();
            if(result.success){
                const data = result.response;
                setOrgData({
                    name: data.name,
                    about: data.about,
                    email: data.email,
                    createdAt: data.createdAt,
                });
            }
            setLoading(false);
        }
        catch(error){
            return showToast("error", "Failed to fetch organization profile data.");
        }
    }

    const handleEdit = () => {
        setEditData(orgData);
        setEditMode(true);
    }

    const handleSubmit = async (e) => {
        e.preventDefault();

        if(editData.password || confirmPassword) {
            if(editData.password !== confirmPassword){
                return showToast("Failed to save", "Passwords do not match.", "error");
            }
        }

        if(!editData.name){
            return showToast("Failed to save", "Name can't be empty", "error");
        }
        if(!editData.about){
            return showToast("Failed to save", "About section can't be empty", "error");
        }
        if(editData.about.length > MAX_LENGTHS.TEXTAREA){
            return showToast("Failed to save", `About section cannot exceed ${MAX_LENGTHS.TEXTAREA} characters.`, "error");
        }
        try{
            setLoading(true);
            const result = await updateOrgProfile(editData);
            if(result.success){
                showToast("success", "Profile updated successfully.");
                getOrgData();
            }
            else{
                showToast("error", "Failed to update profile.");
            }
            setLoading(false);
        }
        catch(error){
            showToast("error", "An error occurred while updating profile.");
        }
        setOrgData(editData);
        setEditMode(false);
    }

    if(loading){
        return(
            <div>
                <NavBar />
                <div className="org-profile-container">
                    <LoadingFallback />
                </div>
            </div>
        ) 
    }
    return(
        <div>
            <NavBar />
            <div className="org-profile-container"> 
                {!editMode ? (
                    <div className="view-mode">
                    <StatCard 
                        title={
                            <div style={{display: "flex", alignItems: "flex-end", gap: "12px"}}>
                                <GoOrganization style={{fontSize: "50px"}}/>
                                <span>{orgData.name}</span>
                            </div>
                        } >
                        <button className="edit-button" onClick={handleEdit}><MdEdit /> Edit Profile</button>
                    </StatCard>
                    <StatCard title="About">
                        <p> {orgData.about}</p>
                    </StatCard>
                        
                    <StatCard title="Founded">
                        <p> {orgData.createdAt.substring(0, 4) || 'N/A'}</p>
                    </StatCard>
                    </div>
                ):(
                    <form onSubmit={handleSubmit}>
                        <StatCard title="Edit Profile">
                            <div className="name">
                                <div className="input-elem">
                                    <label htmlFor="name">Name</label>
                                    <div className="icon-input">
                                        <GoOrganization />
                                        <input type="text" id="name" value={editData.name} minLength={MIN_LENGTHS.INPUT} maxLength={MAX_LENGTHS.INPUT} onChange={(e) => setEditData({...editData, name: e.target.value})} />
                                    </div>
                                </div>
                            </div>

                            <div className="input-elem">
                                <label htmlFor="about">About</label>
                                <div className="icon-input">
                                    <BiMessageSquareDetail />
                                    <textarea id="about" value={editData.about} minLength={MIN_LENGTHS.INPUT} maxLength={MAX_LENGTHS.TEXTAREA} onChange={(e) => setEditData({...editData, about: e.target.value})} />
                                </div>
                            </div>

                            <div className="input-elem">
                                <label htmlFor="password">New Password</label>
                                <div className="icon-input">
                                    <HiOutlineLockClosed />
                                    <input type={showPassword ? "text" : "password"} id="password" name="password" minLength={MIN_LENGTHS.PASSWORD} maxLength={MAX_LENGTHS.INPUT} placeholder="Enter your Password" onChange={(e) => setEditData({...editData, password: e.target.value})}/>
                                    <span className="password-toggle-icon" onClick={() => setShowPassword(!showPassword)} style={{cursor: "pointer"}}>
                                        {showPassword ? <LuEye /> : <LuEyeOff />}
                                    </span>
                                </div>
                            </div>

                            <div className="input-elem">
                                <label htmlFor="confirmPassword">Confirm New Password</label>
                                <div className="icon-input">
                                    <HiOutlineLockClosed />
                                    <input type={showConfirmPassword ? "text" : "password"} id="confirmPassword" name="confirmPassword" minLength={MIN_LENGTHS.PASSWORD} maxLength={MAX_LENGTHS.INPUT} placeholder="Enter your Password" onChange={(e) => setConfirmPassword(e.target.value)}/>
                                    <span className="password-toggle-icon" onClick={() => setShowConfirmPassword(!showConfirmPassword)} style={{cursor: "pointer"}}>
                                        {showConfirmPassword ? <LuEye /> : <LuEyeOff />}
                                    </span>
                                </div>
                            </div>

                            <div className="form-buttons">
                                <button type="button" onClick={() => setEditMode(false)}>Cancel</button>
                                <button type="submit">Save</button>
                            </div>
                        </StatCard>
                    </form>
                )}
                
            </div>
        </div>
    )
}