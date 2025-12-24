import {WatchPartyEventType} from "../constants/constants.jsx";

export const createWistiaAdapter = (video, broadcastAction) => {

    return {
        // The video player can broadcast AND be controlled
        init: () => {
            video.bind("play", () => broadcastAction(WatchPartyEventType.PLAY) );
            video.bind("pause", () => broadcastAction(WatchPartyEventType.PAUSE) );
            video.bind("seek", (currentTime, oldTime) => {
                broadcastAction(WatchPartyEventType.SEEK, {
                    time: currentTime,
                })
            });
            // video.bind("volumechange", () => {
            //     const currentTime = video.time();
            //     video.play(); // may get called internally by Wistia
            //     video.time(currentTime); // restore time immediately
            // })
            // video.bind("end", () => broadcastAction(WATCH_PARTY.END));
        },

        play: () => video.play(),
        pause: () => video.pause(),
        seek: (time) => video.time(time),

        getCurrentTime: () => video.time(),
        isPaused: () => video.state() === "paused",
        isBeforePlay: () => video.state() === "beforeplay",
    }


}