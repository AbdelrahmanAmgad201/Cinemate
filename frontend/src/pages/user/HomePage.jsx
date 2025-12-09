import React from 'react';
import NavBar from '../../components/NavBar';
import Slider from '../../components/Carousel.jsx';

const HomePage = () => {
    return (
        <div style={{ border: '1px dashed red', padding: '20px', textAlign: 'center' }}>
            <h1>Home Page</h1>
            <p>
                When the sidebar is OPEN, I should be centered in the space on the right.<br/>
                When the sidebar is CLOSED, I should be dead center of the screen.
            </p>
        </div>
    );
};

export default HomePage;