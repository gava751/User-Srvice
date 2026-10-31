package com.innoboard.userservice.repository;

import com.innoboard.userservice.domain.PaymentCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PaymentCardRepository extends JpaRepository<PaymentCardEntity, Long> {

    List<PaymentCardEntity> findByUserId(Long userId);

    long countByUserIdAndActiveTrue(Long userId);

    boolean existsByNumber(String number);

    boolean existsByNumberAndIdNot(String number, Long id);

    @Modifying(clearAutomatically = true)
    @Query("update PaymentCardEntity c set c.active = :active, c.updatedAt = :now where c.id = :id")
    int updateActiveStatus(@Param("id") Long id,
                           @Param("active") boolean active,
                           @Param("now") Instant now);
}
