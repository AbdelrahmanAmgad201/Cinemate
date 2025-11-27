import React, { useRef, useState } from 'react';
// Import Swiper React components
import { Swiper, SwiperSlide } from 'swiper/react';

// Import Swiper styles
import 'swiper/css';
import 'swiper/css/pagination';
import 'swiper/css/navigation';
import './style/carousel.css';
import spidermannwh from '../assets/spidermannwh-poster.jpg';
import blackpanther from '../assets/blackpanther-poster.jpg';
import endgame from '../assets/endgame-poster.jpg';
import { MdOutlineStar } from "react-icons/md";


// import required modules
import { Autoplay, Pagination, Navigation } from 'swiper/modules';

export default function Carousel() {
    const images = [
      {
        image: spidermannwh,
        name: "Spider-Man: No Way Home",
        duration: "2h 28min",
        rating: "8.3"
      },
      {
        image: blackpanther,
        name: "Black Panther",
        duration: "2h 15min",
        rating: "7.3"
      },
      {
        image: endgame,
        name: "Avengers: Endgame",
        duration: "3h 1min",
        rating: "8.4"
      }
    ]

    
  return (
    <div className="carousel">
      <Swiper
        spaceBetween={30}
        centeredSlides={true}
        autoplay={{
          delay: 5000,
          disableOnInteraction: false,
        }}
        pagination={{
          clickable: true,
        }}
        navigation={true}
        modules={[Autoplay, Pagination, Navigation]}
        loop={true}
        allowTouchMove={false}
        className="mySwiper"
      >
        {images.map((movie, index) => (
          <SwiperSlide key={index}>
            <div className="img-card"> 
              <div className="img-container">  
                <img src={movie.image} alt={movie.name} />
              </div>
              <div className="overlay"></div>
              <div className="info">
                <h2 className="movie-title">{movie.name}</h2>
                <div className="movie-details">
                  <button className="play-button">Play Now</button>
                  <span className="duration">{movie.duration}</span>
                  <span className="rating" style={{display: "flex",alignItems: "center", gap: "6px"}}><MdOutlineStar style={{color: "#ffc107"}}/> {movie.rating}/10</span>
                </div>
              </div>
            </div>
          </SwiperSlide>
        ))}
      </Swiper>
    </div>
  );
}
