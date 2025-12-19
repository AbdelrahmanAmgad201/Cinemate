package org.example.backend.watchparty;

import lombok.RequiredArgsConstructor;
import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

@Service
@RequiredArgsConstructor
public class WatchPartyService {
    private final MovieRepository movieRepository;
    private final WatchPartyRepository watchPartyRepository;

    public WatchPartyDetailsResponse get(Long userId, String partyId) {
        // call microservice (check if user is member , if yes return room details)
        return null;
    }

    public WatchParty create(Long userId, Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        // Call external Microservice initiate websocket and return partyId
        // if ok -> save to DB and return the code

        return null;
    }

    public void delete(Long userId, String partyId) {
        // call microservice to delete the party
        // remove from DB
        WatchParty watchParty = watchPartyRepository.findByPartyId(partyId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        if(!watchParty.getUserId().equals(userId)){
            throw new RuntimeException("User isn't host");
        }

    }

    public WatchPartyDetailsResponse join(Long userId, String partyId) {
        WatchParty watchParty = watchPartyRepository.findByPartyId(partyId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        // call external microservice and join websocket to delete
        // update watchPartyStatus in watch party entity
        return null;
    }
}
