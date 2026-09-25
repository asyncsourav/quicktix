

package com.asyncsourav.quicktix.security;



import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;





@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {


    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;


    @Override
    protected void doFilterInternal(
            HttpServletRequest request, 
            HttpServletResponse response, 
            FilterChain filterChain
        ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // 1. If no Bearer token, continue to next filter
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract token from header
        String token = authHeader.substring(7);
        /* 
        Suppose: authHeader = Bearer abc123xyz
        Indexes are: Bearer abc123xyz
                     0123456...

        "Bearer " contains 7 characters: and hence we start with 7th Indexes
        */

        String email = jwtUtil.extractEmail(token);

        // 3. If token has valid email and user is not yet authenticated in context
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (jwtUtil.isTokenValid(token, userDetails.getUsername())) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 4. Continue the request chain
        filterChain.doFilter(request, response);
    }
}
