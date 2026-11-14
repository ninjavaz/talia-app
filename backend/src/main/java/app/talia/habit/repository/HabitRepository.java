package app.talia.habit.repository;

import app.talia.habit.entity.HabitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/** Every lookup is scoped by the user: a habit of another user behaves exactly like a missing one. */
public interface HabitRepository extends JpaRepository<HabitEntity, Long> {

    List<HabitEntity> findByUserIdAndActiveTrueOrderBySortOrderAscIdAsc(long userId);

    Optional<HabitEntity> findByIdAndUserIdAndActiveTrue(long id, long userId);

    boolean existsByIdAndUserIdAndActiveTrue(long id, long userId);

    int countByUserIdAndActiveTrue(long userId);

    @Query("select coalesce(max(h.sortOrder), 0) from HabitEntity h where h.userId = :userId and h.active = true")
    int findMaxSortOrder(long userId);
}
