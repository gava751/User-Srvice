package com.innoboard.userservice.repository;

import com.innoboard.userservice.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {

    @Query("select count(u) > 0 from UserEntity u where lower(u.email) = lower(:email)")
    boolean existsByEmailNormalized(@Param("email") String email);

    @Query("select count(u) > 0 from UserEntity u where lower(u.email) = lower(:email) and u.id <> :id")
    boolean existsByEmailNormalizedAndIdNot(@Param("email") String email, @Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("update UserEntity u set u.active = :active, u.updatedAt = :now where u.id = :id")
    int updateActiveStatus(@Param("id") Long id,
                           @Param("active") boolean active,
                           @Param("now") Instant now);
}
