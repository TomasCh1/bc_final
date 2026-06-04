package com.basketball.app.security;

import com.basketball.app.model.User;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Custom permission evaluator for role-based access control
 */
@Component
public class RoleBasedAccessControl implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String requiredRole = permission.toString();
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_" + requiredRole));
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return hasPermission(authentication, targetType, permission);
    }

    /**
     * Check if user has specific role
     */
    public static boolean hasRole(Authentication authentication, User.Role role) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_" + role.name()));
    }

    /**
     * Check if user has any of the specified roles
     */
    public static boolean hasAnyRole(Authentication authentication, User.Role... roles) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        for (User.Role role : roles) {
            if (hasRole(authentication, role)) {
                return true;
            }
        }
        return false;
    }
}

