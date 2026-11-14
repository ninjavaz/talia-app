package app.talia.measurement.service;

import app.talia.common.NotFoundException;
import app.talia.measurement.model.BodyPart;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.List;

/** Body measurements the user defined (waist, hips, biceps...); a new account has none. */
@Service
public class BodyPartService {

    private final JdbcClient jdbc;

    BodyPartService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<BodyPart> list(long userId) {
        return jdbc.sql("""
                        SELECT id, name, sort_order FROM body_part
                        WHERE user_id = :user AND active ORDER BY sort_order, id
                        """)
                .param("user", userId)
                .query((rs, n) -> new BodyPart(rs.getLong(1), rs.getString(2), rs.getInt(3)))
                .list();
    }

    public BodyPart create(long userId, String name) {
        long id = jdbc.sql("""
                        INSERT INTO body_part (user_id, name, sort_order)
                        VALUES (:user, :name,
                                (SELECT COALESCE(max(sort_order), 0) + 1 FROM body_part WHERE user_id = :user AND active))
                        RETURNING id
                        """)
                .param("user", userId)
                .param("name", name.trim())
                .query(Long.class)
                .single();
        return find(userId, id);
    }

    public BodyPart rename(long userId, long id, String name) {
        if (jdbc.sql("UPDATE body_part SET name = :name WHERE id = :id AND user_id = :user AND active")
                .param("name", name.trim()).param("id", id).param("user", userId).update() == 0) {
            throw new NotFoundException("Body part not found");
        }
        return find(userId, id);
    }

    /** Soft delete: past measurements stay in the database but are no longer shown. */
    public void delete(long userId, long id) {
        if (jdbc.sql("UPDATE body_part SET active = FALSE WHERE id = :id AND user_id = :user AND active")
                .param("id", id).param("user", userId).update() == 0) {
            throw new NotFoundException("Body part not found");
        }
    }

    public boolean isActive(long userId, long id) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM body_part WHERE id = :id AND user_id = :user AND active)")
                .param("id", id).param("user", userId).query(Boolean.class).single();
    }

    private BodyPart find(long userId, long id) {
        return jdbc.sql("SELECT id, name, sort_order FROM body_part WHERE id = :id AND user_id = :user AND active")
                .param("id", id).param("user", userId)
                .query((rs, n) -> new BodyPart(rs.getLong(1), rs.getString(2), rs.getInt(3)))
                .optional()
                .orElseThrow(() -> new NotFoundException("Body part not found"));
    }
}
