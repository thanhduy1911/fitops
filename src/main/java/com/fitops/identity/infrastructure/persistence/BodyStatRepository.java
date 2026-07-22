package com.fitops.identity.infrastructure.persistence;

import com.fitops.identity.domain.entity.BodyStat;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BodyStatRepository extends JpaRepository<BodyStat, UUID> {
  Page<BodyStat> findByUserIdOrderByRecordedAtDesc(UUID userId, Pageable pageable);
}
