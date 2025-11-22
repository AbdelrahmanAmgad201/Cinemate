package org.example.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.backend.user.Gender;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.example.backend.security.JWTProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    @Autowired
    private JWTProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException{
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");
        String googleId = oAuth2User.getAttribute("sub");
        String googleGender = oAuth2User.getAttribute("gender");
        final Gender gender = parseGender(googleGender);

        // Check if user exists, if not create new user
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
            .email(email)
            .firstName(firstName)
            .lastName(lastName)
            .provider("google")
            .providerId(googleId)
            .gender(gender)
            .password(null)
            .build();

            return userRepository.save(newUser);
        });

        // Update user info if changed
        if(user.getId() != null){
            boolean needsUpdate = false;
            
            if(!firstName.equals(user.getFirstName())){
                user.setFirstName(firstName);
                needsUpdate = true;
            }

            if(!lastName.equals(user.getLastName())){
                user.setLastName(lastName);
                needsUpdate = true;
            }

            if(gender != null && !gender.equals(user.getGender())){
                user.setGender(gender);
                needsUpdate = true;
            }
            
            if(needsUpdate){
                userRepository.save(user);
            }
        }

        String token = jwtProvider.generateToken(user);

        String redirectUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/oauth2/redirect")
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        
    }

    private Gender parseGender(String googleGender){
        if(googleGender == null){
            return null;
        }
        try{
            return Gender.valueOf(googleGender.toUpperCase());
        }
        catch(IllegalArgumentException e){
            return null;
        }
    }
}
