import React , { useState }from 'react';
import NavBar from '../../components/NavBar';
import PostCard from '../../components/PostCard.jsx';
import pic from "../../assets/action.jpg";
import { IoIosPerson } from "react-icons/io";
import "./style/HomePage.css"

const HomePage = () => {

    return (
        <div>
            {/* <NavBar /> */}
            <div className="posts-list">
                {/* {posts.map((post, index) => (
                    <PostCard key={index} postBody={post} />
                ))} */}
                HomePage
            </div>
        </div>
    );
};

export default HomePage;