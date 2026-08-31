package com.maxime.help.msauth.infrastructure.security;

import com.maxime.help.msauth.domain.port.out.TokenHasher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/** Deterministic SHA-256 hashing for opaque, high-entropy tokens (e.g. refresh tokens). */
@Component
class Sha256TokenHasherAdapter implements TokenHasher {

    @Override
    public String hash(String rawValue) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256").digest(rawValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
