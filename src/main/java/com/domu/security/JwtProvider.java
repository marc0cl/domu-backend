package com.domu.security;

import com.domu.domain.core.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class JwtProvider {

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final String issuer;
    private final Long expirationMinutes;

    public JwtProvider(String secret, String issuer, Long expirationMinutes) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build();
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(String.valueOf(user.id()))
                .withIssuedAt(now)
                .withExpiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
                .withClaim("email", user.email())
                .withClaim("roleId", user.roleId())
                .withClaim("unitId", user.unitId())
                .sign(algorithm);
    }

    public DecodedJWT verify(String token) throws JWTVerificationException {
        return verifier.verify(token);
    }
}
