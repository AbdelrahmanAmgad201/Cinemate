import React , { useState }from 'react';
import NavBar from '../../components/NavBar';
import PostCard from '../../components/PostCard.jsx';
import pic from "../../assets/action.jpg";
import { IoIosPerson } from "react-icons/io";
import "./style/HomePage.css"

const HomePage = () => {

    const [posts, setPosts] = useState([
        { 
            userId: 1,
            avatar: <IoIosPerson />,
            firstName: "Sam",
            lastName: "Jonas",
            time: "22-11-2025",
            title: "Wish There Was A Second Season",
            media: pic,
            text: "Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. ",
            votes: 1234,
            postId: 1
        },
        { 
            userId: 2,
            firstName: "Jane",
            lastName: "Doe",
            time: "08-12-2024",
            title: "I liked This Scene A Lot",
            media: pic,
            votes: 543,
            postId: 2
        },
        { 
            userId: 3,
            firstName: "John",
            lastName: "Smith",
            time: "09-12-2024",
            text: "Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. ",
            title: "My Top Movies!",
            votes: 892,
            postId: 3
        }
    ]);

    return (
        <div>
            {/* <NavBar /> */}
            <div className="posts-list">
                {posts.map((post, index) => (
                    <PostCard key={index} postBody={post} />
                ))}
            </div>
        </div>
    );
};

export default HomePage;