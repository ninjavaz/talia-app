package app.talia.measurement.repository;

import app.talia.measurement.entity.BodyPartEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/** Every lookup is scoped by the user: a body part of another user behaves exactly like a missing one. */
public interface BodyPartRepository extends JpaRepository<BodyPartEntity, Long> {

    List<BodyPartEntity> findByUserIdAndActiveTrueOrderBySortOrderAscIdAsc(long userId);

    Optional<BodyPartEntity> findByIdAndUserIdAndActiveTrue(long id, long userId);

    boolean existsByIdAndUserIdAndActiveTrue(long id, long userId);

    @Query("select coalesce(max(b.sortOrder), 0) from BodyPartEntity b where b.userId = :userId and b.active = true")
    int findMaxSortOrder(long userId);
}
