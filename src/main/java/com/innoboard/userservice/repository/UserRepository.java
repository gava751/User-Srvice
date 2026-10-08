package com.innoboard.userservice.repository;

import com.innoboard.userservice.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("select u from UserEntity u where u.surname = :surname and u.active = true")
    List<UserEntity> findActiveBySurname(@Param("surname") String surname);

    @Query(value = "select * from users where extract(year from birth_date) = :year",
            nativeQuery = true)
    List<UserEntity> findByBirthYear(@Param("year") int year);
}
