package app.talia.sync.repository;

import app.talia.sync.entity.SyncLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncLogRepository extends JpaRepository<SyncLogEntity, Long> {
}
