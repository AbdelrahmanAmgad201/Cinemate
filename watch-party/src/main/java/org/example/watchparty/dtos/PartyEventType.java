package org.example.watchparty.dtos;

public enum PartyEventType {
    // Playback control events
    PLAY,
    PAUSE,
    SEEK,

    // Chat event
    CHAT,

    // Member lifecycle events
    USER_JOINED,
    USER_LEFT,
    PARTY_DELETED
}

