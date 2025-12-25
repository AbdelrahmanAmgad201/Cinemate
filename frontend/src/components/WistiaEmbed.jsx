import {useRef, useEffect} from 'react'

const WISTIA_SCRIPT  = `https://fast.wistia.com/assets/external/E-v1.js`;

function loadWistiaScript() {
    if (document.querySelector(`script[src="${WISTIA_SCRIPT}"]`)) return;
    const s = document.createElement("script");
    s.src = WISTIA_SCRIPT;
    s.async = true;
    document.body.appendChild(s);
}

export default function WistiaEmbed({ wistiaId, className = "", onReady }) {
    const containerRef = useRef(null);
    useEffect(() => {
        if (!wistiaId) return;
        loadWistiaScript();

        // get wisita player instance
        window._wq = window._wq || [];
        window._wq.push({
            id: wistiaId,
            onReady: function (video){
                // `video` is the player instance
                if (onReady) {
                    onReady(video);
                }
            }

        })
    }, [wistiaId, onReady]);

    return (
        <div
            ref={containerRef}
            className={`wistia_embed wistia_async_${wistiaId} ${className}`}
            style={{ width: "100%", height: "100%" }}
            aria-label="video player"
            data-video-options='{"autoplay": false}'
        />

    )

}