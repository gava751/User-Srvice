package com.innoboard.userservice.repository;

import com.innoboard.userservice.domain.PaymentCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentCardRepository extends JpaRepository<PaymentCardEntity, Long> {

    List<PaymentCardEntity> findByUserId(Long userId);

    long countByUserId(Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    @Query("select c from PaymentCardEntity c where c.user.id = :userId and c.active = :active")
    List<PaymentCardEntity> findAllByUserIdAndActive(@Param("userId") Long userId,
                                                     @Param("active") boolean active);

    @Query(value = "select * from payment_cards where user_id = :userId and expiration_date < current_date",
            nativeQuery = true)
    List<PaymentCardEntity> findExpiredByUserId(@Param("userId") Long userId);
}
