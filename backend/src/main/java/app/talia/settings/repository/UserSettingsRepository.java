package app.talia.settings.repository;

import app.talia.settings.entity.UserSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingsRepository extends JpaRepository<UserSettingsEntity, Long> {
}
