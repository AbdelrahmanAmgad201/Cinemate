package org.example.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.example.backend.user.OAuthUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    /**
     * The frontend base URL (e.g. http://localhost:5173 in dev, https://cinemate.example.com in prod).
     * After a successful Google login, the backend redirects to
     *   ${app.frontend.url}/oauth2/redirect?token=<JWT>
     * so the frontend can pick up the token.
     */
    @Value("${app.frontend.url}")
    private String frontendUrl;

    public OAuthSuccessHandler(JWTProvider jwtProvider, UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        OAuthUser oauthUser = OAuthUser.from(oAuth2User);

        // Check if user exists; if not, create a new OAuth user record
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

        // Redirect to the frontend OAuth landing page with the JWT as a query param.
        // frontendUrl is injected from app.frontend.url (set via FRONTEND_URL env var).
        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("token", token)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
