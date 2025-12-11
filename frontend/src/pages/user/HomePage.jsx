import React , { useState }from 'react';
import NavBar from '../../components/NavBar';
import PostCard from '../../components/PostCard.jsx';
import pic from "../../assets/action.jpg";
import { IoIosPerson } from "react-icons/io";
import "./style/HomePage.css"

const HomePage = () => {

    const posts = [
        { 
            userId: 1,
            avatar: <IoIosPerson />,
            firstName: "Sam",
            lastName: "Jonas",
            time: "22-11-2025",
            title: "This show deserves more recognition",
            media: pic,
            text: `Just finished binge-watching the entire season and I'm blown away. The character development, the cinematography, the soundtrack - everything was perfect.
    
    I can't believe they haven't announced a second season yet. The cliffhanger ending left so many questions unanswered. Anyone else feel the same way?`,
            votes: 1234,
            postId: "507f1f77bcf86cd799439011", // Changed to ObjectId format
            forumId: "507f1f77bcf86cd799439001"
        },
        { 
            userId: 2,
            firstName: "Jane",
            lastName: "Doe",
            time: "08-12-2024",
            title: "The cinematography in this scene is absolutely stunning",
            media: pic,
            text: `The way they used lighting and color grading here is masterful. You can feel the emotion without a single word being spoken. This is what visual storytelling is all about.`,
            votes: 543,
            postId: "507f1f77bcf86cd799439012", // Changed to ObjectId format
            forumId: "507f1f77bcf86cd799439001"
        },
        { 
            userId: 3,
            firstName: "John",
            lastName: "Smith",
            time: "09-12-2024",
            text: `After watching countless films this year, I've finally compiled my top 10 list. These movies really stood out for their storytelling, performances, and overall impact.
    
    Would love to hear what made your list this year! Any hidden gems I should check out?`,
            title: "My Top 10 Movies of 2024",
            votes: 892,
            postId: "507f1f77bcf86cd799439013", // Changed to ObjectId format
            forumId: "507f1f77bcf86cd799439001"
        },
        { 
            userId: 4,
            firstName: "Emily",
            lastName: "Chen",
            time: "10-12-2024",
            title: "Unpopular opinion: The ending was perfect",
            text: `I know a lot of people were disappointed, but I think the ambiguous ending was exactly what the story needed. Not everything needs to be wrapped up in a neat bow.
    
    It leaves room for interpretation and gives us something to think about long after the credits roll. That's the mark of great storytelling.`,
            votes: 267,
            postId: "507f1f77bcf86cd799439014", // Changed to ObjectId format
            forumId: "507f1f77bcf86cd799439001"
        },
        { 
            userId: 5,
            firstName: "Marcus",
            lastName: "Williams",
            time: "11-12-2024",
            title: "Found this gem at a thrift store today",
            media: pic,
            text: `Can't believe I found the original poster in mint condition! This movie has been my comfort watch for years. Sometimes the best finds are completely unexpected.`,
            votes: 1567,
            postId: "507f1f77bcf86cd799439015", // Changed to ObjectId format
            forumId: "507f1f77bcf86cd799439001"
        }
        ];

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