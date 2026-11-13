package com.becafinance.mstransactions.adapter.out.persistence.repository;
import com.becafinance.mstransactions.adapter.out.persistence.entity.AccountJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountJpaEntity a where a.userId = :userId")
    Optional<AccountJpaEntity> lockByUserId(@Param("userId") long userId);
}
