package app.talia.measurement;

import app.talia.common.BadRequestException;
import app.talia.common.NotFoundException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.List;

/** User-defined body measurements. The built-in waist (code WAIST) can be renamed but not deleted. */
@Service
class BodyPartService {

    static final String WAIST = "WAIST";

    record BodyPart(long id, String name, String code, int sortOrder) {
    }

    private final JdbcClient jdbc;

    BodyPartService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<BodyPart> list() {
        return jdbc.sql("SELECT id, name, code, sort_order FROM body_part WHERE active ORDER BY sort_order, id")
                .query((rs, n) -> new BodyPart(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getInt(4)))
                .list();
    }

    BodyPart create(String name) {
        long id = jdbc.sql("""
                        INSERT INTO body_part (name, sort_order)
                        VALUES (:name, (SELECT COALESCE(max(sort_order), 0) + 1 FROM body_part WHERE active))
                        RETURNING id
                        """)
                .param("name", name.trim())
                .query(Long.class)
                .single();
        return find(id);
    }

    BodyPart rename(long id, String name) {
        if (jdbc.sql("UPDATE body_part SET name = :name WHERE id = :id AND active")
                .param("name", name.trim()).param("id", id).update() == 0) {
            throw new NotFoundException("Body part not found");
        }
        return find(id);
    }

    /** Soft delete: past measurements stay in the database but are no longer shown. */
    void delete(long id) {
        BodyPart part = find(id);
        if (WAIST.equals(part.code())) {
            throw new BadRequestException("The waist measurement cannot be deleted");
        }
        jdbc.sql("UPDATE body_part SET active = FALSE WHERE id = :id").param("id", id).update();
    }

    boolean isActive(long id) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM body_part WHERE id = :id AND active)")
                .param("id", id).query(Boolean.class).single();
    }

    private BodyPart find(long id) {
        return jdbc.sql("SELECT id, name, code, sort_order FROM body_part WHERE id = :id AND active")
                .param("id", id)
                .query((rs, n) -> new BodyPart(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getInt(4)))
                .optional()
                .orElseThrow(() -> new NotFoundException("Body part not found"));
    }
}
