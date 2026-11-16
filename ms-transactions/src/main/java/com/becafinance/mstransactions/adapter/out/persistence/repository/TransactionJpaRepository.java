package com.becafinance.mstransactions.adapter.out.persistence.repository;
import com.becafinance.mstransactions.adapter.out.persistence.entity.TransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface TransactionJpaRepository extends JpaRepository<TransactionJpaEntity, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<TransactionJpaEntity> {
    Optional<TransactionJpaEntity> findByIdAndUserId(long id, long userId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from TransactionJpaEntity t where t.id=:id and t.userId=:userId")
    Optional<TransactionJpaEntity> lockOwned(@org.springframework.data.repository.query.Param("id") long id,
        @org.springframework.data.repository.query.Param("userId") long userId);
}
