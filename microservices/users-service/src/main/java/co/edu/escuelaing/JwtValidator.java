package co.edu.escuelaing;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtValidator {
    private final String domain;
    private final String audience;

    public JwtValidator(String domain, String audience) {
        this.domain = domain;
        this.audience = audience;
    }

    public Map<String, String> validate(String token) {
        try {
            DecodedJWT decoded = JWT.decode(token);
            JwkProvider provider = new UrlJwkProvider(new URL("https://" + domain + "/.well-known/jwks.json"));
            Jwk jwk = provider.get(decoded.getKeyId());
            Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);

            JWT.require(algorithm)
                    .withIssuer("https://" + domain + "/")
                    .withAudience(audience)
                    .build()
                    .verify(token);

            Map<String, String> claims = new HashMap<>();
            claims.put("sub", decoded.getSubject());
            claims.put("email", decoded.getClaim("email").asString());
            claims.put("name", decoded.getClaim("name").asString());
            claims.put("picture", decoded.getClaim("picture").asString());
            return claims;
        } catch (Exception e) {
            return null;
        }
    }
}
