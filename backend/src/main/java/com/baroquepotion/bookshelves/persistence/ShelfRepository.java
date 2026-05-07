package com.baroquepotion.bookshelves.persistence;

import com.baroquepotion.bookshelves.domain.Shelf;
import com.baroquepotion.bookshelves.domain.ShelfCoordinateAudit;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * JDBC repository for shelf vocabulary entries.
 */
@Repository
public class ShelfRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ShelfRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns all shelf rows ordered by name.
     *
     * @return shelf definitions
     */
    public List<Shelf> findAll() {
        return jdbcTemplate.query("""
                        select s.id, s.name, s.audited, count(b.id) as usage_count
                        from shelf s
                        left join book b on b.shelf_id = s.id
                        group by s.id, s.name, s.audited
                        order by s.name asc
                        """,
                (rs, rowNum) -> new Shelf(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getBoolean("audited"),
                        rs.getLong("usage_count")));
    }

    /**
     * Returns occupied shelf coordinates with their persisted audit state.
     *
     * @return distinct shelf coordinate audit rows ordered by shelf and coordinate
     */
    public List<ShelfCoordinateAudit> findCoordinateAudits() {
        return jdbcTemplate.query("""
                        select s.id as shelf_id,
                               s.name as shelf_name,
                               b.shelf_coords,
                               coalesce(sca.audited, false) as audited
                        from shelf s
                        inner join (
                            select distinct shelf_id, shelf_coords
                            from book
                            where shelf_id is not null
                              and shelf_coords is not null
                              and btrim(shelf_coords) <> ''
                        ) b on b.shelf_id = s.id
                        left join shelf_coordinate_audit sca
                          on sca.shelf_id = s.id
                         and sca.shelf_coords = b.shelf_coords
                        order by s.name asc, b.shelf_coords asc
                        """,
                (rs, rowNum) -> new ShelfCoordinateAudit(
                        rs.getString("shelf_id"),
                        rs.getString("shelf_name"),
                        rs.getString("shelf_coords"),
                        rs.getBoolean("audited")));
    }

    /**
     * Checks whether a shelf exists.
     *
     * @param id shelf identifier
     * @return {@code true} when the row exists
     */
    public boolean existsById(String id) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from shelf where id = :id",
                Map.of("id", id),
                Integer.class);
        return count != null && count > 0;
    }

    /**
     * Inserts a new shelf row.
     *
     * @param shelf shelf values to persist
     */
    public void insert(Shelf shelf) {
        jdbcTemplate.update("""
                        insert into shelf (id, name, audited)
                        values (:id, :name, :audited)
                        """,
                Map.of("id", shelf.id(), "name", shelf.name(), "audited", shelf.audited()));
    }

    /**
     * Counts how many books currently reference a shelf.
     *
     * @param id shelf identifier
     * @return number of books assigned to the shelf
     */
    public long usageCount(String id) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from book where shelf_id = :id",
                Map.of("id", id),
                Long.class);
        return count == null ? 0 : count;
    }

    /**
     * Updates an existing shelf row, including its identifier.
     *
     * @param existingId identifier of the row to update
     * @param shelf replacement values
     */
    public void update(String existingId, Shelf shelf) {
        jdbcTemplate.update("""
                        update shelf
                        set id = :newId,
                            name = :name,
                            audited = :audited
                        where id = :existingId
                        """,
                Map.of("existingId", existingId, "newId", shelf.id(), "name", shelf.name(), "audited", shelf.audited()));
    }

    /**
     * Upserts the audit state for one shelf coordinate.
     *
     * @param shelfId shelf identifier
     * @param shelfCoords coordinate tier on the shelf
     * @param audited audit state to persist
     */
    public void updateCoordinateAudit(String shelfId, String shelfCoords, boolean audited) {
        jdbcTemplate.update("""
                        insert into shelf_coordinate_audit (shelf_id, shelf_coords, audited)
                        values (:shelfId, :shelfCoords, :audited)
                        on conflict (shelf_id, shelf_coords)
                        do update set audited = excluded.audited
                        """,
                Map.of("shelfId", shelfId, "shelfCoords", shelfCoords, "audited", audited));
    }

    /**
     * Deletes a shelf row.
     *
     * @param id shelf identifier
     */
    public void delete(String id) {
        jdbcTemplate.update("delete from shelf where id = :id", Map.of("id", id));
    }
}
