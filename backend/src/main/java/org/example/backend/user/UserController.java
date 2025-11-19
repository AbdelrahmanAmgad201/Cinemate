package org.example.backend.user;

import org.example.backend.verification.Verfication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private  UserService userService;

    @PostMapping("/v1/sign-up")
    public Verfication signUp(@RequestBody SignUpDTO signUpDTO) {
        return userService.signUp(signUpDTO);
    }
}