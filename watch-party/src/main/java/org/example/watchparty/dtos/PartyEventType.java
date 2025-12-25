package org.example.watchparty.dtos;

public enum PartyEventType {
    // Playback control events
    PLAY,
    PAUSE,
    SEEK,
    // Clients can request the host to send the current time for resynchronization
    SYNC_REQUEST,

    // Chat event
    CHAT,

    // Member lifecycle events
    USER_JOINED,
    USER_LEFT,
    PARTY_DELETED
}
