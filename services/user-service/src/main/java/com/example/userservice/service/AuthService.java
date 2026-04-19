package com.example.userservice.service;

import com.example.userservice.dto.JwtResponse;
import com.example.userservice.dto.LoginRequest;
import com.example.userservice.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserService userService;

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        
        String jwt = jwtUtils.generateToken(userDetails);
        
        com.example.userservice.dto.UserResponse userResponse = userService.getUserByUsername(userDetails.getUsername());
        
        return new JwtResponse(
            jwt,
            userResponse.getId(),
            userResponse.getUsername(),
            userResponse.getEmail(),
            userResponse.getFirstName(),
            userResponse.getLastName()
        );
    }
}