import {WatchPartyEventType} from "../constants/constants.jsx";

export const createWistiaAdapter = (video, broadcastAction) => {
    let hasUnlockedPlayback = false;

    return {
        // The video player can broadcast AND be controlled
        init: () => {
            video.bind("play", () => {
                if (!hasUnlockedPlayback) {
                    hasUnlockedPlayback = true;
                    broadcastAction(WatchPartyEventType.SYNC_REQUEST);
                    return;
                }

                broadcastAction(WatchPartyEventType.SYNC_REQUEST);
                broadcastAction(WatchPartyEventType.PLAY);
            } );
            video.bind("pause", () => broadcastAction(WatchPartyEventType.PAUSE) );
            video.bind("seek", (currentTime, oldTime) => {
                if (video.state() === "beforeplay") return;

                broadcastAction(WatchPartyEventType.SEEK, {
                    time: currentTime,
                })
            });
        },

        // The following functions to control the player/get info
        play: () => video.play(),
        pause: () => video.pause(),
        seek: (time) => video.time(time),

        getCurrentTime: () => video.time(),
        isPaused: () => video.state() === "paused",
        isBeforePlay: () => video.state() === "beforeplay",
        videoObject: () => video,
    }


}