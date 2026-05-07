package com.baroquepotion.bookshelves.persistence;

import com.baroquepotion.bookshelves.domain.Collection;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * JDBC repository for collection vocabulary entries.
 */
@Repository
public class CollectionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CollectionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns all collection rows ordered by name.
     *
     * @return collection definitions
     */
    public List<Collection> findAll() {
        return jdbcTemplate.query("""
                        select c.id, c.name, count(bc.book_id) as usage_count
                        from collection c
                        left join book_collection bc on bc.collection_id = c.id
                        group by c.id, c.name
                        order by c.name asc
                        """,
                (rs, rowNum) -> new Collection(rs.getString("id"), rs.getString("name"), rs.getLong("usage_count")));
    }

    /**
     * Checks whether a collection exists.
     *
     * @param id collection identifier
     * @return {@code true} when the row exists
     */
    public boolean existsById(String id) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from collection where id = :id",
                Map.of("id", id),
                Integer.class);
        return count != null && count > 0;
    }

    /**
     * Inserts a new collection row.
     *
     * @param collection collection values to persist
     */
    public void insert(Collection collection) {
        jdbcTemplate.update("""
                        insert into collection (id, name)
                        values (:id, :name)
                        """,
                Map.of("id", collection.id(), "name", collection.name()));
    }

    /**
     * Counts how many books currently reference a collection.
     *
     * @param id collection identifier
     * @return number of book assignments that use the collection
     */
    public long usageCount(String id) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from book_collection where collection_id = :id",
                Map.of("id", id),
                Long.class);
        return count == null ? 0 : count;
    }

    /**
     * Updates an existing collection row, including its identifier.
     *
     * @param existingId identifier of the row to update
     * @param collection replacement values
     */
    public void update(String existingId, Collection collection) {
        jdbcTemplate.update("""
                        update collection
                        set id = :newId,
                            name = :name
                        where id = :existingId
                        """,
                Map.of("existingId", existingId, "newId", collection.id(), "name", collection.name()));
    }

    /**
     * Deletes a collection row.
     *
     * @param id collection identifier
     */
    public void delete(String id) {
        jdbcTemplate.update("delete from collection where id = :id", Map.of("id", id));
    }
}
