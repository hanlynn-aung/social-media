package com.example.socialmedia.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to check if the current user is the owner of the resource or is an admin.
 * Used in conjunction with path variables to authorize resource modifications.
 * 
 * Example: @RequireResourceOwner("userId")
 * This will check if the current user's ID matches the userId path variable.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireResourceOwner {
    /**
     * The path variable name that contains the user ID to check ownership
     */
    String value();
}
