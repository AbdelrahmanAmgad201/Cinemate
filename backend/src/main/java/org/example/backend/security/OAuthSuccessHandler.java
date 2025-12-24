package org.example.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.example.backend.user.OAuthUser;
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
    private UserRepository userRepository;

    public OAuthSuccessHandler(JWTProvider jwtProvider, UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException{
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        OAuthUser oauthUser = OAuthUser.from(oAuth2User);

        // Check if user exists, if not create new user
        User user = userRepository.findByEmail(oauthUser.getEmail()).orElseGet(() -> {
            User newUser = User.builder()
            .email(oauthUser.getEmail())
            .firstName(oauthUser.getFirstName())
            .lastName(oauthUser.getLastName())
            .provider("google")
            .providerId(oauthUser.getGoogleId())
            .gender(null)
            .birthDate(null)
            .password(null)
            .profileComplete(false)
            .build();

            return userRepository.save(newUser);
        });

        String token = jwtProvider.generateToken(user);

        String redirectUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/oauth2/redirect")
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        
    }
}
