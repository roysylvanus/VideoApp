package com.example.videoapp.video

/**
 * Represents the current state of the video call connection.
 */
enum class ConnectionState {

    /** Initial state before joining a call */
    IDLE,

    /** Establishing a new session connection */
    CONNECTING,

    /** Successfully connected to the session */
    CONNECTED,

    /** Unexpected disconnection (e.g. network issue) */
    DISCONNECTED,

    /** Attempting to recover connection after interruption */
    RECONNECTING,

    /** Error occurred while connecting or during the call */
    ERROR
}