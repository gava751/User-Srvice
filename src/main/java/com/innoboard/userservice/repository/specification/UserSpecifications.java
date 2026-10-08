package com.innoboard.userservice.repository.specification;

import com.innoboard.userservice.domain.UserEntity;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<UserEntity> hasName(String name) {
        return (root, query, cb) ->
                name == null ? cb.conjunction() : cb.equal(cb.lower(root.get("name")), name.toLowerCase());
    }

    public static Specification<UserEntity> hasSurname(String surname) {
        return (root, query, cb) ->
                surname == null ? cb.conjunction() : cb.equal(cb.lower(root.get("surname")), surname.toLowerCase());
    }

    public static Specification<UserEntity> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }
}
