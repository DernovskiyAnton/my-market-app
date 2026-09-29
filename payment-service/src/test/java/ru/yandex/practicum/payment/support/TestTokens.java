package ru.yandex.practicum.payment.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public final class TestTokens {

    public static final String ISSUER = "http://localhost:9000";
    public static final String AUDIENCE = "payment-service";

    private static final RSAKey SIGNING_KEY = generateKey("test-key");
    private static final RSAKey FOREIGN_KEY = generateKey("test-key");
    private static final MockWebServer JWKS_SERVER = startJwksServer();

    private TestTokens() {
    }

    public static String jwkSetUri() {
        return "http://" + JWKS_SERVER.getHostName() + ":" + JWKS_SERVER.getPort() + "/oauth2/jwks";
    }

    public static String valid() {
        return builder().sign();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String issuer = ISSUER;
        private List<String> audience = List.of(AUDIENCE);
        private List<String> scopes = List.of("payments");
        private Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));
        private RSAKey key = SIGNING_KEY;

        public Builder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public Builder audience(String... audience) {
            this.audience = List.of(audience);
            return this;
        }

        public Builder scopes(String... scopes) {
            this.scopes = List.of(scopes);
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder signedWithForeignKey() {
            this.key = FOREIGN_KEY;
            return this;
        }

        public String sign() {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject("market-app")
                    .audience(audience)
                    .claim("scope", scopes)
                    .issueTime(Date.from(expiresAt.minus(Duration.ofMinutes(5))))
                    .notBeforeTime(Date.from(expiresAt.minus(Duration.ofMinutes(5))))
                    .expirationTime(Date.from(expiresAt))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
            try {
                jwt.sign(new RSASSASigner(key));
            } catch (JOSEException e) {
                throw new IllegalStateException(e);
            }
            return jwt.serialize();
        }
    }

    private static RSAKey generateKey(String keyId) {
        try {
            return new RSAKeyGenerator(2048).keyID(keyId).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static MockWebServer startJwksServer() {
        MockWebServer server = new MockWebServer();
        String jwks = new JWKSet(SIGNING_KEY.toPublicJWK()).toString();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setHeader("Content-Type", "application/json").setBody(jwks);
            }
        });
        try {
            server.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return server;
    }
}
