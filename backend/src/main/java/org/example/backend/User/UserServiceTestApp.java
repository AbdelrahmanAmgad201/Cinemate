package org.example.backend.User;

import org.example.backend.User.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication(scanBasePackages = {"org.example.backend"})
public class UserServiceTestApp {

    public static void main(String[] args) {
        // Start Spring Boot context
        ApplicationContext context = SpringApplication.run(UserServiceTestApp.class, args);

        // Get the UserService bean (Spring injects UserRepository automatically)
        UserService userService = context.getBean(UserService.class);

        // Test signUp
        SignUpDTO dto = new SignUpDTO();
        dto.setEmail("test@example.com");
        dto.setPassword("123456");

        try {
            User user = userService.signUp(dto);
            System.out.println("User signed up: " + user.getEmail());
        } catch (RuntimeException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Optional: close the Spring context
        SpringApplication.exit(context);
    }
}
