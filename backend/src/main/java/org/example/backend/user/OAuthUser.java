package org.example.backend.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Getter
@Builder
@AllArgsConstructor
public class OAuthUser {
    private String email;
    private String firstName;
    private String lastName;
    private String googleId;

    public static OAuthUser from(OAuth2User oAuth2User){
        return OAuthUser.builder()
            .email(oAuth2User.getAttribute("email"))
            .firstName(oAuth2User.getAttribute("given_name"))
            .lastName(oAuth2User.getAttribute("family_name"))
            .googleId(oAuth2User.getAttribute("sub"))
            .build();
    }
}
