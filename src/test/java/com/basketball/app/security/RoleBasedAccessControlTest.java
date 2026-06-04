package com.basketball.app.security;

import com.basketball.app.model.User;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class RoleBasedAccessControlTest {

    private Authentication authWith(String... roles) {
        return new UsernamePasswordAuthenticationToken(
                "user",
                "pwd",
                List.of(roles).stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList()
        );
    }

    @Test
    void hasPermission_MatchesRole() {
        RoleBasedAccessControl evaluator = new RoleBasedAccessControl();
        assertTrue(evaluator.hasPermission(authWith("ADMIN"), null, "ADMIN"));
        assertFalse(evaluator.hasPermission(authWith("PLAYER"), null, "ADMIN"));
    }

    @Test
    void staticHelpers_WorkAsExpected() {
        Authentication auth = authWith("TRAINER", "PLAYER");
        assertTrue(RoleBasedAccessControl.hasRole(auth, User.Role.TRAINER));
        assertTrue(RoleBasedAccessControl.hasAnyRole(auth, User.Role.ADMIN, User.Role.PLAYER));
        assertFalse(RoleBasedAccessControl.hasAnyRole(auth, User.Role.ADMIN));
    }
}
