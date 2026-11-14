package app.talia.measurement.service;

import app.talia.common.NotFoundException;
import app.talia.measurement.entity.BodyPartEntity;
import app.talia.measurement.model.BodyPart;
import app.talia.measurement.repository.BodyPartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Body measurements the user defined (waist, hips, biceps...); a new account has none. */
@Service
@Transactional
public class BodyPartService {

    private final BodyPartRepository bodyParts;

    BodyPartService(BodyPartRepository bodyParts) {
        this.bodyParts = bodyParts;
    }

    @Transactional(readOnly = true)
    public List<BodyPart> list(long userId) {
        return bodyParts.findByUserIdAndActiveTrueOrderBySortOrderAscIdAsc(userId).stream()
                .map(BodyPartService::toModel)
                .toList();
    }

    public BodyPart create(long userId, String name) {
        int sortOrder = bodyParts.findMaxSortOrder(userId) + 1;
        return toModel(bodyParts.save(new BodyPartEntity(userId, name.trim(), sortOrder)));
    }

    public BodyPart rename(long userId, long id, String name) {
        BodyPartEntity part = find(userId, id);
        part.setName(name.trim());
        return toModel(part);
    }

    /** Soft delete: past measurements stay in the database but are no longer shown. */
    public void delete(long userId, long id) {
        find(userId, id).deactivate();
    }

    @Transactional(readOnly = true)
    public boolean isActive(long userId, long id) {
        return bodyParts.existsByIdAndUserIdAndActiveTrue(id, userId);
    }

    private BodyPartEntity find(long userId, long id) {
        return bodyParts.findByIdAndUserIdAndActiveTrue(id, userId)
                .orElseThrow(() -> new NotFoundException("Body part not found"));
    }

    private static BodyPart toModel(BodyPartEntity e) {
        return new BodyPart(e.getId(), e.getName(), e.getSortOrder());
    }
}
