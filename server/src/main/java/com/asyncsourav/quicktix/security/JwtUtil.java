


package com.asyncsourav.quicktix.security;



import com.asyncsourav.quicktix.entity.Role;
import com.asyncsourav.quicktix.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;




/**
 * Utility component for creating, signing, parsing, and validating JSON Web Tokens (JWT).
 * Uses the HMAC-SHA256 (HS256) signature algorithm with JJWT 0.12.x standards.
 */
@Slf4j
@Component
public class JwtUtil {


    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretKey;

    @Value("${jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;


    /**
     * Generates a signed JWT token for the authenticated user.
     *
     * @param user The authenticated user entity
     * @return Compact signed JWT string
     */
    public String generateToken(User user) {

        Map<String, Object> extraClaims = new HashMap<>();

        extraClaims.put("userId", user.getId());
        extraClaims.put("role", user.getRole().name());

        return buildToken(
                extraClaims, 
                user.getEmail(), 
                jwtExpirationMs
        );
    }



    /**
     * Generates a signed JWT token with explicit subject and custom claims.
     *
     * @param email User email (Subject)
     * @param userId User primary key
     * @param role User role enum
     * @return Compact signed JWT string
     */
    public String generateToken(String email, Long userId, Role role) {

        Map<String, Object> extraClaims = new HashMap<>();
        
        extraClaims.put("userId", userId);
        extraClaims.put("role", role.name());

        return buildToken(
                extraClaims, 
                email, 
                jwtExpirationMs
        );
    }



    /**
     * Constructs and signs the JWT token using JJWT 0.12.x builder API.
     */
    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs) {

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }



    /**
     * Extracts the subject (email) from the token.
     */
    public String extractEmail(String token) {

        return extractClaim(token, claims -> {
            return claims.getSubject();
        });
    }

    
    /**
     * Extracts the userId claim from the token.
     */
    public Long extractUserId(String token) {

        return extractClaim(token, claims -> {
            Object userId = claims.get("userId");

            if (userId instanceof Number number) 
                return number.longValue();

            return null;
        });
    }


    /**
     * Extracts the role claim from the token.
     */
    public String extractRole(String token) {

        return extractClaim(token, claims -> {
            return claims.get("role", String.class);
        });
    }


    /**
     * Extracts the token expiration timestamp.
     */
    public Date extractExpiration(String token) {
        
        return extractClaim(token, claim -> {
            return claim.getExpiration();
        });
    }


    /**
     * Extracts a specific claim using a custom claims resolver function.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }


    /**
     * Validates whether a token belongs to the specified email and has not expired.
     */
    public boolean isTokenValid(String token, String expectedEmail) {

        final String email = extractEmail(token);

        return email != null
                && email.equalsIgnoreCase(expectedEmail)
                && !isTokenExpired(token);
    }


    /**
     * Validates token signature and expiration integrity.
     */
    public boolean validateToken(String token) {

        try {
            extractAllClaims(token);
            return true;
        } 
        
        catch (JwtException | IllegalArgumentException ex) {
            log.warn(
                    "Invalid JWT token: {}", 
                    ex.getMessage()
            );
            return false;
        }
    }


    /**
     * Checks if the token expiration timestamp is in the past.
     */
    public boolean isTokenExpired(String token) {

        Date expiration = extractExpiration(token);
        return expiration != null 
                && expiration.before(new Date());
    }


    /**
     * Parses and verifies the signed JWT payload using the secret signing key.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    /**
     * Generates a cryptographic SecretKey from the secret configuration string.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretKey);
        } catch (Exception e) {
            keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
