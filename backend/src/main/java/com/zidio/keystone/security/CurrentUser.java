package com.zidio.keystone.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Small helper so services/controllers can read who's making the request without repeating boilerplate. */
@Component
public class CurrentUser {

    public KeystoneUserPrincipal get() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof KeystoneUserPrincipal p) {
            return p;
        }
        throw new IllegalStateException("No authenticated KEYSTONE user in context");
    }

    public Long id() { return get().getId(); }

    public String role() { return get().getRole(); }

    public Long customerId() { return get().getCustomerId(); }
}
