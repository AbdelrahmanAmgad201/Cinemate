package org.example.backend.vote;

public interface Votable {
    void incrementUpvote();
    void incrementDownvote();
    void decrementUpvote();
    void decrementDownvote();
}