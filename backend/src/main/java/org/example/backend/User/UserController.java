package org.example.backend.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private  UserService userService;

    @PostMapping("/signUp")
    public User signUp(@RequestBody SignUpDTO signUpDTO) {
        System.out.println("signUp");
        return userService.signUp(signUpDTO);
    }
}