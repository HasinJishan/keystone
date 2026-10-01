package com.zidio.keystone.security;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory blocklist of "killed" JWTs, as taught in class (ConcurrentHashMap).
 * A JWT is stateless and stays valid until it expires, so on logout we remember the
 * token here and JwtAuthFilter rejects it on every later request.
 *
 * ConcurrentHashMap-backed set = safe when many requests hit it at the same time.
 * Note: it lives in memory, so it is cleared when the server restarts.
 */
@Component
public class TokenKillService {

    private final Set<String> blockedTokens =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    public void blockListToken(String token) {
        blockedTokens.add(token);
    }

    public boolean isBlocked(String token) {
        return blockedTokens.contains(token);
    }
}
