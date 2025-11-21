package org.example.backend.verification;

import org.example.backend.user.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

@Service
public class VerificationService {

    @Autowired
    private UserRepository userRepository;
    private final VerificationRepository verificationRepository;

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email}")
    private String fromEmail;

    private final RestTemplate restTemplate = new RestTemplate();

    public VerificationService(VerificationRepository verificationRepository) {
        this.verificationRepository = verificationRepository;
    }

    private void addUser(String email, String password) {
        User user = User.builder()
                .email(email)
                .password(password)
                .build();
        userRepository.save(user);
    }

    private Optional<Verfication> verify(String Email, int code) {
        Optional<Verfication> verification = verificationRepository.findByEmail(Email);
        try {
            if (verification.isPresent()) {
                int verifiedCode =verification.get().getCode();
                if (verifiedCode == code) {
                    return verification;
                }
                else  {
                    return Optional.empty();
                }
            }
            else {
                throw new RuntimeException("email not found in DB");
            }
        }catch (Exception e){
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public boolean sendVerificationEmail(String toEmail, int code) {
        try {
            String url = "https://api.sendgrid.com/v3/mail/send";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(sendGridApiKey);

            Map<String, Object> body = new HashMap<>();

            body.put("from", Map.of("email", fromEmail));

            body.put("personalizations", List.of(
                    Map.of(
                            "to", List.of(Map.of("email", toEmail)),
                            "subject", "Your Verification Code"
                    )
            ));

            body.put("content", List.of(
                    Map.of(
                            "type", "text/plain",
                            "value", "Your verification code is: " + code
                    )
            ));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(url, request, String.class);

            return true;

        } catch (Exception e) {
            e.printStackTrace(); // <--- IMPORTANT
            return false;
        }
    }


    public boolean verifyEmail(VerificationDTO verificationDTO) {
        try {
            String email = verificationDTO.getEmail();
            int code = verificationDTO.getCode();
            Optional<Verfication> verification = verify(email, code);
            if (verification.isPresent()) {
                String password = verification.get().getPassword();
                addUser(email, password);
                verificationRepository.delete(verification.get());
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
