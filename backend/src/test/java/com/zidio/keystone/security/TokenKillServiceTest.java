package com.zidio.keystone.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenKillServiceTest {

    @Test
    void tokenIsNotBlockedUntilLogout() {
        TokenKillService service = new TokenKillService();
        assertFalse(service.isBlocked("abc.def.ghi"));
    }

    @Test
    void loggedOutTokenIsBlocked() {
        TokenKillService service = new TokenKillService();
        service.blockListToken("abc.def.ghi");
        assertTrue(service.isBlocked("abc.def.ghi"));
        assertFalse(service.isBlocked("another.token"));
    }
}
