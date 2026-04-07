package com.support.desk.service;

import com.support.desk.dto.JwtRequest;
import com.support.desk.dto.JwtResponse;
import com.support.desk.jwt.JwtAuthenticationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Service
public class AuthService {

    private static final Logger logger = LogManager.getLogger(AuthService.class);

    @Autowired
    AuthenticationManager manager;

    private JwtAuthenticationHelper jwtHelper;

    public AuthService(JwtAuthenticationHelper jwtAuthenticationHelper) {
        this.jwtHelper = jwtAuthenticationHelper;
    }

    @Autowired
    UserDetailsService userDetailsService;

    public JwtResponse login(JwtRequest jwtRequest) {

        logger.info("Attempting login for user: {}", jwtRequest.getUsername());

        //authenticate with Authentication manager
        this.doAuthenticate(jwtRequest.getUsername(),jwtRequest.getPassword());

        UserDetails userDetails = userDetailsService.loadUserByUsername(jwtRequest.getUsername());
        String token = jwtHelper.generateToken(userDetails);

        JwtResponse response = JwtResponse.builder().jwtToken(token).build();

        logger.info("Login successful for user: {}", jwtRequest.getUsername());
        return response;
    }

    private void doAuthenticate(String username, String password) {

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
        try {
            logger.info("Authenticating user: {}", username);
            manager.authenticate(authenticationToken);

        }catch (BadCredentialsException e) {
            logger.error("Authentication failed for user: {}", username, e);
            throw new BadCredentialsException("Invalid Username or Password");
        }
    }

}