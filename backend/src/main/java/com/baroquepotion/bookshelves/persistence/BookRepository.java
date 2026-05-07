package com.baroquepotion.bookshelves.persistence;

import com.baroquepotion.bookshelves.domain.Book;
import com.baroquepotion.bookshelves.domain.BookNavigation;
import com.baroquepotion.bookshelves.domain.BookSearchCriteria;
import com.baroquepotion.bookshelves.domain.BookSearchResult;
import com.baroquepotion.bookshelves.domain.BookSummary;
import com.baroquepotion.bookshelves.domain.CollectionValueReportRow;
import com.baroquepotion.bookshelves.domain.PublisherSummary;
import com.baroquepotion.bookshelves.domain.ValueAnalyticsCriteria;
import com.baroquepotion.bookshelves.domain.ValueDetail;
import com.baroquepotion.bookshelves.domain.ValueSummary;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC repository for books and their collection assignments.
 */
@Repository
public class BookRepository {

    public static final String UNSPECIFIED_AUTHOR_KEY = "__unspecified__";
    private static final String EXCLUDED_COLLECTION_ID = "excluded";

    private static final RowMapper<BookSummary> SUMMARY_ROW_MAPPER = (rs, rowNum) ->
            new BookSummary(
                    rs.getObject("id", UUID.class),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("shelf_coords"),
                    (Integer) rs.getObject("year"),
                    rs.getString("shelf_id"),
                    rs.getDate("date_added") == null ? null : rs.getDate("date_added").toLocalDate(),
                    rs.getObject("updated_at", OffsetDateTime.class)
            );

    private static final RowMapper<Book> BOOK_ROW_MAPPER = new BookRowMapper();
    private static final RowMapper<ValueDetail> VALUE_DETAIL_ROW_MAPPER = (rs, rowNum) ->
            new ValueDetail(
                    rs.getObject("id", UUID.class),
                    rs.getString("title"),
                    rs.getString("author"),
                    (Integer) rs.getObject("year"),
                    rs.getString("shelf_id"),
                    rs.getDate("date_added") == null ? null : rs.getDate("date_added").toLocalDate(),
                    rs.getObject("updated_at", OffsetDateTime.class),
                    rs.getBigDecimal("value_amount"),
                    rs.getObject("price_to_replace_checked", LocalDate.class),
                    rs.getBoolean("special"),
                    rs.getBoolean("digital"),
                    (Boolean) rs.getObject("hardback"),
                    rs.getBoolean("dustjacket"),
                    rs.getBoolean("slipcase"),
                    rs.getBoolean("video")
            );
    private static final RowMapper<CollectionValueReportRow> COLLECTION_VALUE_REPORT_ROW_MAPPER = (rs, rowNum) ->
            new CollectionValueReportRow(
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getLong("book_count"),
                    rs.getBigDecimal("total_value")
            );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public BookRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Executes a filtered book search for the catalog list view.
     *
     * @param criteria search filters and paging values
     * @return matching book summaries ordered by title
     */
    public BookSearchResult search(BookSearchCriteria criteria) {
        SearchQuery searchQuery = buildSearchQuery(criteria);

        Long totalCount = jdbcTemplate.queryForObject(
                "select count(*) " + searchQuery.fromAndWhere(),
                searchQuery.params(),
                Long.class
        );

        var sql = new StringBuilder("""
                select b.id, b.title, b.author, b.shelf_coords, b.year, b.shelf_id, b.date_added, b.updated_at
                """);
        sql.append(searchQuery.fromAndWhere());
        sql.append(" order by ");
        sql.append(orderByClause(criteria));
        sql.append(", b.title asc, b.id asc limit :limit offset :offset");
        Map<String, Object> params = new HashMap<>(searchQuery.params());
        params.put("limit", criteria.size());
        params.put("offset", criteria.offset());
        List<BookSummary> books = jdbcTemplate.query(sql.toString(), params, SUMMARY_ROW_MAPPER);
        return new BookSearchResult(books, totalCount == null ? 0 : totalCount);
    }

    /**
     * Returns the previous and next book ids around one current book within the
     * full filtered and sorted catalog result set.
     *
     * @param currentBookId current book identifier
     * @param criteria active filters and sort order
     * @return neighboring navigation metadata when the current book is still in scope
     */
    public Optional<BookNavigation> findNavigation(UUID currentBookId, BookSearchCriteria criteria) {
        SearchQuery searchQuery = buildSearchQuery(criteria);
        String orderedSearch = orderByClause(criteria) + ", b.title asc, b.id asc";
        Map<String, Object> params = new HashMap<>(searchQuery.params());
        params.put("currentBookId", currentBookId);
        return jdbcTemplate.query("""
                        with filtered as (
                            select b.id,
                                   lag(b.id) over (order by %s) as previous_book_id,
                                   lead(b.id) over (order by %s) as next_book_id,
                                   row_number() over (order by %s) as position,
                                   count(*) over () as total_items
                            %s
                        )
                        select previous_book_id, next_book_id, position, total_items
                        from filtered
                        where id = :currentBookId
                        """.formatted(orderedSearch, orderedSearch, orderedSearch, searchQuery.fromAndWhere()),
                params,
                rs -> rs.next()
                        ? Optional.of(new BookNavigation(
                        rs.getObject("previous_book_id", UUID.class),
                        rs.getObject("next_book_id", UUID.class),
                        ((Number) rs.getObject("position")).intValue(),
                        ((Number) rs.getObject("total_items")).longValue()))
                        : Optional.empty());
    }

    /**
     * Returns author buckets for the author browse view.
     *
     * @return authors ordered by label
     */
    public List<AuthorSummary> listAuthors() {
        return jdbcTemplate.query("""
                        select case
                                   when nullif(btrim(coalesce(author, '')), '') is null then :unspecifiedKey
                                   else nullif(btrim(coalesce(author, '')), '')
                               end as author_key,
                               coalesce(nullif(btrim(coalesce(author, '')), ''), '(Unspecified)') as author_name,
                               count(*) as book_count
                        from book
                        group by nullif(btrim(coalesce(author, '')), '')
                        order by lower(coalesce(nullif(btrim(coalesce(author, '')), ''), '(Unspecified)')) asc
                        """,
                Map.of("unspecifiedKey", UNSPECIFIED_AUTHOR_KEY),
                (rs, rowNum) -> new AuthorSummary(
                        rs.getString("author_key"),
                        rs.getString("author_name"),
                        rs.getLong("book_count")));
    }

    /**
     * Returns all books for one author bucket.
     *
     * @param authorKey author bucket key returned by {@link #listAuthors()}
     * @return books for that author ordered by title
     */
    public List<BookSummary> findBooksByAuthorKey(String authorKey) {
        String whereClause;
        Map<String, Object> params = new HashMap<>();
        if (UNSPECIFIED_AUTHOR_KEY.equals(authorKey)) {
            whereClause = "where btrim(coalesce(b.author, '')) = ''";
        } else {
            whereClause = "where b.author = :author";
            params.put("author", authorKey);
        }
        return jdbcTemplate.query("""
                        select b.id, b.title, b.author, b.shelf_coords, b.year, b.shelf_id, b.date_added, b.updated_at
                        from book b
                        """ + whereClause + """
                         order by lower(b.title) asc, b.title asc, b.id asc
                        """,
                params,
                SUMMARY_ROW_MAPPER);
    }

    /**
     * Returns the most recently added books.
     *
     * @param limit maximum number of books to return
     * @return books ordered by date added descending
     */
    public List<BookSummary> findRecentlyAdded(int limit) {
        return jdbcTemplate.query("""
                        select b.id, b.title, b.author, b.shelf_coords, b.year, b.shelf_id, b.date_added, b.updated_at
                        from book b
                        order by b.date_added desc nulls last, b.created_at desc, lower(b.title) asc, b.id asc
                        limit :limit
                        """,
                Map.of("limit", Math.max(limit, 1)),
                SUMMARY_ROW_MAPPER);
    }

    /**
     * Returns the most recently updated books.
     *
     * @param limit maximum number of books to return
     * @return books ordered by update timestamp descending
     */
    public List<BookSummary> findRecentlyUpdated(int limit) {
        return jdbcTemplate.query("""
                        select b.id, b.title, b.author, b.shelf_coords, b.year, b.shelf_id, b.date_added, b.updated_at
                        from book b
                        order by b.updated_at desc nulls last, lower(b.title) asc, b.id asc
                        limit :limit
                        """,
                Map.of("limit", Math.max(limit, 1)),
                SUMMARY_ROW_MAPPER);
    }

    /**
     * Returns all books that currently have a shelf assignment.
     *
     * @return shelf-assigned books ordered by shelf and title
     */
    public List<BookSummary> findBooksByShelf() {
        return jdbcTemplate.query("""
                        select b.id, b.title, b.author, b.shelf_coords, b.year, b.shelf_id, b.date_added, b.updated_at
                        from book b
                        where b.shelf_id is not null and b.shelf_id <> ''
                        order by b.shelf_id asc,
                                 lower(coalesce(substring(nullif(btrim(b.shelf_coords), '') from '^[A-Za-z]+'), '{')) asc,
                                 coalesce((substring(nullif(btrim(b.shelf_coords), '') from '([0-9]+)$'))::integer, 2147483647) asc,
                                 lower(coalesce(nullif(btrim(b.shelf_coords), ''), '{')) asc,
                                 lower(coalesce(b.author, '')) asc,
                                 lower(b.title) asc,
                                 b.title asc,
                                 b.id asc
                        """,
                SUMMARY_ROW_MAPPER);
    }

    /**
     * Returns all books grouped via collection membership.
     *
     * @return rows whose shelfId field temporarily carries collection ids for grouping
     */
    public List<BookSummary> findBooksByCollection() {
        return jdbcTemplate.query("""
                        select b.id, b.title, b.author, b.shelf_coords, b.year, bc.collection_id as shelf_id, b.date_added, b.updated_at
                        from book b
                        join book_collection bc on bc.book_id = b.id
                        order by bc.collection_id asc, lower(b.title) asc, b.id asc
                        """,
                SUMMARY_ROW_MAPPER);
    }

    /**
     * Returns collection-level value totals for the printable sunk-cost report.
     *
     * @return rows ordered by collection name
     */
    public List<CollectionValueReportRow> findValueByCollectionPaidReport() {
        return findValueByCollectionReport("paid");
    }

    /**
     * Returns collection-level value totals for the printable insurance-value report.
     *
     * @return rows ordered by collection name
     */
    public List<CollectionValueReportRow> findValueByCollectionReplacementReport() {
        return findValueByCollectionReport("replacement");
    }

    /**
     * Returns books that currently do not have a shelf assignment.
     *
     * @return books missing a physical location
     */
    public List<BookSummary> findMissingLocationBooks() {
        return jdbcTemplate.query("""
                        select b.id, b.title, b.author, b.shelf_coords, b.year, b.shelf_id, b.date_added, b.updated_at
                        from book b
                        where b.shelf_id is null or b.shelf_id = ''
                        order by lower(b.title) asc, b.id asc
                        """,
                SUMMARY_ROW_MAPPER);
    }

    /**
     * Returns all non-blank publisher strings with usage counts.
     *
     * @return publisher buckets ordered alphabetically
     */
    public List<PublisherSummary> listPublishers() {
        return jdbcTemplate.query("""
                        select b.publisher, count(*) as book_count
                        from book b
                        where nullif(btrim(coalesce(b.publisher, '')), '') is not null
                        group by b.publisher
                        order by lower(b.publisher) asc, b.publisher asc
                        """,
                (rs, rowNum) -> new PublisherSummary(
                        rs.getString("publisher"),
                        rs.getLong("book_count")));
    }

    /**
     * Rewrites one exact publisher string across all matching books.
     *
     * @param sourcePublisher current publisher string
     * @param targetPublisher replacement publisher string
     * @return number of updated books
     */
    public long renamePublisher(String sourcePublisher, String targetPublisher) {
        Integer updated = jdbcTemplate.update("""
                        update book
                        set publisher = :targetPublisher
                        where publisher = :sourcePublisher
                        """,
                new MapSqlParameterSource()
                        .addValue("sourcePublisher", sourcePublisher)
                        .addValue("targetPublisher", targetPublisher));
        return updated == null ? 0 : updated.longValue();
    }

    /**
     * Returns aggregate monetary totals and coverage statistics for one analytics scope.
     *
     * @param criteria selected basis and scope filters
     * @return aggregate summary for the matching books
     */
    public ValueSummary fetchValueSummary(ValueAnalyticsCriteria criteria) {
        AnalyticsQuery analyticsQuery = buildAnalyticsQuery(criteria);
        String valueColumn = valueColumn(criteria);
        return jdbcTemplate.queryForObject("""
                        select coalesce(sum(%s), 0) as total_value,
                               count(%s) as included_count,
                               count(*) - count(%s) as missing_value_count,
                               count(*) as total_scoped_count,
                               count(*) filter (where b.price_to_replace is not null and b.price_to_replace_checked is null)
                                   as missing_replacement_checked_count,
                               min(b.price_to_replace_checked) filter (where b.price_to_replace is not null and b.price_to_replace_checked is not null)
                                   as oldest_replacement_checked
                        %s
                        """.formatted(valueColumn, valueColumn, valueColumn, analyticsQuery.fromAndWhere()),
                analyticsQuery.params(),
                (rs, rowNum) -> new ValueSummary(
                        rs.getBigDecimal("total_value"),
                        rs.getLong("included_count"),
                        rs.getLong("missing_value_count"),
                        rs.getLong("total_scoped_count"),
                        rs.getLong("missing_replacement_checked_count"),
                        rs.getObject("oldest_replacement_checked", LocalDate.class)
                ));
    }

    /**
     * Returns scoped book rows with the selected monetary basis projected into one column.
     *
     * @param criteria selected basis and scope filters
     * @return scoped book rows ordered for inspection
     */
    public List<ValueDetail> fetchValueDetails(ValueAnalyticsCriteria criteria) {
        AnalyticsQuery analyticsQuery = buildAnalyticsQuery(criteria);
        String valueColumn = valueColumn(criteria);
        return jdbcTemplate.query("""
                        select b.id, b.title, b.author, b.year, b.shelf_id, b.date_added, b.updated_at,
                               %s as value_amount,
                               b.price_to_replace_checked,
                               b.special,
                               b.digital,
                               b.hardback,
                               b.dustjacket,
                               b.slipcase,
                               b.video
                        %s
                        order by lower(coalesce(b.author, '')) asc, lower(b.title) asc, b.title asc, b.id asc
                        """.formatted(valueColumn, analyticsQuery.fromAndWhere()),
                analyticsQuery.params(),
                VALUE_DETAIL_ROW_MAPPER);
    }

    private String orderByClause(BookSearchCriteria criteria) {
        String direction = criteria.normalizedSortDirection();
        return switch (criteria.normalizedSortBy()) {
            case "author" -> "lower(coalesce(b.author, '')) " + direction;
            case "year" -> "b.year " + direction;
            case "dateAdded" -> "b.date_added " + direction;
            case "updated" -> "b.updated_at " + direction;
            default -> "lower(b.title) " + direction;
        };
    }

    private SearchQuery buildSearchQuery(BookSearchCriteria criteria) {
        var fromAndWhere = new StringBuilder("""
                from book b
                where 1 = 1
                """);
        Map<String, Object> params = new HashMap<>();

        if (criteria.query() != null && !criteria.query().isBlank()) {
            fromAndWhere.append("""
                     and (b.title ilike :query
                          or b.author ilike :query
                          or b.author_secondary ilike :query
                          or b.series ilike :query)
                    """);
            params.put("query", "%" + criteria.query() + "%");
        }
        appendCollectionFilter(fromAndWhere, params, criteria);
        if (Boolean.TRUE.equals(criteria.noBestEstimateOnly())) {
            fromAndWhere.append("""
                     and coalesce(nullif(b.price_to_replace, 0), nullif(b.price_paid, 0), nullif(b.price_on_item, 0), 0) = 0
                    """);
        }
        if (criteria.shelfId() != null && !criteria.shelfId().isBlank()) {
            fromAndWhere.append(" and b.shelf_id = :shelfId");
            params.put("shelfId", criteria.shelfId());
        }
        if (criteria.year() != null) {
            fromAndWhere.append(" and b.year = :year");
            params.put("year", criteria.year());
        }
        if (criteria.hasShelf() != null) {
            if (criteria.hasShelf()) {
                fromAndWhere.append(" and b.shelf_id is not null and b.shelf_id <> ''");
            } else {
                fromAndWhere.append(" and (b.shelf_id is null or b.shelf_id = '')");
            }
        }
        appendAnySelectedFlags(fromAndWhere, criteria.specialOnly(), criteria.digitalOnly(), criteria.hardbackOnly(),
                criteria.dustjacketOnly(), criteria.slipcaseOnly(), criteria.videoOnly());
        if (criteria.dateAddedOn() != null) {
            switch (criteria.normalizedDateAddedMode()) {
                case "before" -> {
                    fromAndWhere.append(" and b.date_added is not null and b.date_added < :dateAddedOn");
                    params.put("dateAddedOn", criteria.dateAddedOn());
                }
                case "after" -> {
                    fromAndWhere.append(" and b.date_added is not null and b.date_added > :dateAddedOn");
                    params.put("dateAddedOn", criteria.dateAddedOn());
                }
                default -> {
                }
            }
        }
        if ("year".equals(criteria.normalizedDateAddedMode()) && criteria.dateAddedYear() != null) {
            fromAndWhere.append(" and b.date_added is not null and extract(year from b.date_added) = :dateAddedYear");
            params.put("dateAddedYear", criteria.dateAddedYear());
        }
        switch (criteria.normalizedLastAuditedMode()) {
            case "before" -> {
                if (criteria.lastAuditedOn() != null) {
                    fromAndWhere.append(" and b.last_audited is not null and b.last_audited < :lastAuditedOn");
                    params.put("lastAuditedOn", criteria.lastAuditedOn());
                }
            }
            case "after" -> {
                if (criteria.lastAuditedOn() != null) {
                    fromAndWhere.append(" and b.last_audited is not null and b.last_audited > :lastAuditedOn");
                    params.put("lastAuditedOn", criteria.lastAuditedOn());
                }
            }
            case "never" -> fromAndWhere.append(" and b.last_audited is null");
            default -> {
            }
        }
        return new SearchQuery(fromAndWhere.toString(), params);
    }

    private void appendCollectionFilter(StringBuilder fromAndWhere,
                                        Map<String, Object> params,
                                        BookSearchCriteria criteria) {
        if (Boolean.TRUE.equals(criteria.noCollectionOnly())) {
            fromAndWhere.append("""
                     and not exists (
                        select 1
                        from book_collection bc
                        where bc.book_id = b.id
                    )
                    """);
            return;
        }
        var collectionIds = criteria.collectionIds() == null ? List.<String>of() : criteria.collectionIds();
        if (collectionIds.isEmpty()) {
            return;
        }
        if ("and".equals(criteria.normalizedCollectionMode()) && collectionIds.size() > 1) {
            fromAndWhere.append("""
                     and (
                        select count(distinct bc.collection_id)
                        from book_collection bc
                        where bc.book_id = b.id
                          and bc.collection_id in (:collectionIds)
                    ) = :collectionCount
                    """);
            params.put("collectionIds", collectionIds);
            params.put("collectionCount", collectionIds.size());
        } else {
            fromAndWhere.append("""
                     and exists (
                        select 1
                        from book_collection bc
                        where bc.book_id = b.id
                          and bc.collection_id in (:collectionIds)
                    )
                    """);
            params.put("collectionIds", collectionIds);
        }
    }

    private AnalyticsQuery buildAnalyticsQuery(ValueAnalyticsCriteria criteria) {
        var fromAndWhere = new StringBuilder("""
                from book b
                where 1 = 1
                """);
        Map<String, Object> params = new HashMap<>();
        if (criteria.collectionId() != null && !criteria.collectionId().isBlank()) {
            fromAndWhere.append("""
                     and exists (
                        select 1
                        from book_collection bc
                        where bc.book_id = b.id
                          and bc.collection_id = :collectionId
                    )
                    """);
            params.put("collectionId", criteria.collectionId());
        }
        if (criteria.shelfId() != null && !criteria.shelfId().isBlank()) {
            fromAndWhere.append(" and b.shelf_id = :shelfId");
            params.put("shelfId", criteria.shelfId());
        }
        if (criteria.dateAddedOn() != null) {
            switch (criteria.normalizedDateAddedMode()) {
                case "before" -> {
                    fromAndWhere.append(" and b.date_added is not null and b.date_added < :dateAddedOn");
                    params.put("dateAddedOn", criteria.dateAddedOn());
                }
                case "after" -> {
                    fromAndWhere.append(" and b.date_added is not null and b.date_added > :dateAddedOn");
                    params.put("dateAddedOn", criteria.dateAddedOn());
                }
                default -> {
                }
            }
        }
        if ("year".equals(criteria.normalizedDateAddedMode()) && criteria.dateAddedYear() != null) {
            fromAndWhere.append(" and b.date_added is not null and extract(year from b.date_added) = :dateAddedYear");
            params.put("dateAddedYear", criteria.dateAddedYear());
        }
        appendAnySelectedFlags(fromAndWhere, criteria.specialOnly(), criteria.digitalOnly(), criteria.hardbackOnly(),
                criteria.dustjacketOnly(), criteria.slipcaseOnly(), criteria.videoOnly());
        return new AnalyticsQuery(fromAndWhere.toString(), params);
    }

    private String valueColumn(ValueAnalyticsCriteria criteria) {
        String baseColumn = switch (criteria.normalizedBasis()) {
            case "printed" -> "b.price_on_item";
            case "replacement" -> "b.price_to_replace";
            case "bestEstimate" -> "coalesce(nullif(b.price_to_replace, 0), nullif(b.price_paid, 0), nullif(b.price_on_item, 0), 0)";
            default -> "b.price_paid";
        };
        if (EXCLUDED_COLLECTION_ID.equals(criteria.collectionId())) {
            return baseColumn;
        }
        return """
                case
                    when exists (
                        select 1
                        from book_collection excluded_bc
                        where excluded_bc.book_id = b.id
                          and excluded_bc.collection_id = 'excluded'
                    ) then 0
                    else %s
                end
                """.formatted(baseColumn);
    }

    private List<CollectionValueReportRow> findValueByCollectionReport(String reportType) {
        String baseColumn = switch (reportType) {
            case "replacement" -> "coalesce(nullif(b.price_to_replace, 0), nullif(b.price_paid, 0), nullif(b.price_on_item, 0), 0)";
            default -> "coalesce(nullif(b.price_paid, 0), nullif(b.price_on_item, 0), nullif(b.price_to_replace, 0), 0)";
        };
        return jdbcTemplate.query("""
                        select c.id,
                               c.name,
                               count(bc.book_id) as book_count,
                               coalesce(sum(
                                   case
                                       when b.id is null then 0
                                       when c.id = 'excluded' then %s
                                       when exists (
                                           select 1
                                           from book_collection excluded_bc
                                           where excluded_bc.book_id = b.id
                                             and excluded_bc.collection_id = 'excluded'
                                       ) then 0
                                       else %s
                                   end
                               ), 0) as total_value
                        from collection c
                        left join book_collection bc on bc.collection_id = c.id
                        left join book b on b.id = bc.book_id
                        group by c.id, c.name
                        order by total_value desc, lower(c.name) asc, c.name asc, c.id asc
                        """.formatted(baseColumn, baseColumn),
                Map.of(),
                COLLECTION_VALUE_REPORT_ROW_MAPPER);
    }

    private void appendAnySelectedFlags(StringBuilder sql,
                                        Boolean specialSelected,
                                        Boolean digitalSelected,
                                        Boolean hardbackSelected,
                                        Boolean dustjacketSelected,
                                        Boolean slipcaseSelected,
                                        Boolean videoSelected) {
        List<String> predicates = new ArrayList<>();
        if (Boolean.TRUE.equals(specialSelected)) {
            predicates.add("b.special = true");
        }
        if (Boolean.TRUE.equals(digitalSelected)) {
            predicates.add("b.digital = true");
        }
        if (Boolean.TRUE.equals(hardbackSelected)) {
            predicates.add("b.hardback = true");
        }
        if (Boolean.TRUE.equals(dustjacketSelected)) {
            predicates.add("b.dustjacket = true");
        }
        if (Boolean.TRUE.equals(slipcaseSelected)) {
            predicates.add("b.slipcase = true");
        }
        if (Boolean.TRUE.equals(videoSelected)) {
            predicates.add("b.video = true");
        }
        if (!predicates.isEmpty()) {
            sql.append(" and (");
            sql.append(String.join(" or ", predicates));
            sql.append(")");
        }
    }

    private record SearchQuery(String fromAndWhere, Map<String, Object> params) {
    }

    private record AnalyticsQuery(String fromAndWhere, Map<String, Object> params) {
    }

    /**
     * Loads a single book and its collection assignments.
     *
     * @param id unique book identifier
     * @return populated book when present
     */
    public Optional<Book> findById(UUID id) {
        var books = jdbcTemplate.query("""
                        select id, title, author, publication_city, publisher, imprint, year, first_published, edition, pages, frontpages, date_added,
                               last_audited, shelf_id, author_secondary, isbn, special, digital, hardback, dustjacket, slipcase, video, vols, series, shelf_coords,
                               note, price_paid, price_on_item, price_to_replace, price_to_replace_checked,
                               created_at, updated_at
                        from book
                        where id = :id
                        """,
                Map.of("id", id),
                BOOK_ROW_MAPPER);
        if (books.isEmpty()) {
            return Optional.empty();
        }
        var book = books.getFirst();
        var collectionIds = jdbcTemplate.queryForList("""
                        select collection_id
                        from book_collection
                        where book_id = :bookId
                        order by collection_id
                        """,
                Map.of("bookId", id),
                String.class);
        return Optional.of(new Book(
                book.id(),
                book.title(),
                book.author(),
                book.publicationCity(),
                book.publisher(),
                book.imprint(),
                book.year(),
                book.firstPublished(),
                book.edition(),
                book.pages(),
                book.frontpages(),
                book.dateAdded(),
                book.lastAudited(),
                book.shelfId(),
                book.authorSecondary(),
                book.isbn(),
                book.special(),
                book.digital(),
                book.hardback(),
                book.dustjacket(),
                book.slipcase(),
                book.video(),
                book.vols(),
                book.series(),
                book.shelfCoords(),
                book.note(),
                book.pricePaid(),
                book.priceOnItem(),
                book.priceToReplace(),
                book.priceToReplaceChecked(),
                book.createdAt(),
                book.updatedAt(),
                collectionIds
        ));
    }

    /**
     * Checks whether a book exists.
     *
     * @param id unique book identifier
     * @return {@code true} when the row exists
     */
    public boolean existsById(UUID id) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from book where id = :id",
                Map.of("id", id),
                Integer.class);
        return count != null && count > 0;
    }

    /**
     * Inserts a new book row and persists its collection assignments.
     *
     * @param book book values to persist
     * @return generated book identifier
     */
    public UUID insert(Book book) {
        UUID id = jdbcTemplate.queryForObject("""
                        insert into book (
                            title, author, publication_city, publisher, imprint, year, first_published, edition, pages, frontpages, date_added, last_audited, shelf_id,
                            author_secondary, isbn, special, digital, hardback, dustjacket, slipcase, video, vols, series, shelf_coords, note, price_paid,
                            price_on_item, price_to_replace, price_to_replace_checked
                        ) values (
                            :title, :author, :publicationCity, :publisher, :imprint, :year, :firstPublished, :edition, :pages, :frontpages, :dateAdded, :lastAudited, :shelfId,
                            :authorSecondary, :isbn, :special, :digital, :hardback, :dustjacket, :slipcase, :video, :vols, :series, :shelfCoords, :note, :pricePaid,
                            :priceOnItem, :priceToReplace, :priceToReplaceChecked
                        )
                        returning id
                        """,
                toParams(book),
                (rs, rowNum) -> rs.getObject("id", UUID.class));
        replaceCollections(id, book.collectionIds());
        return id;
    }

    /**
     * Updates an existing book row and replaces its collection assignments.
     *
     * @param book book values to persist
     */
    public void update(Book book) {
        jdbcTemplate.update("""
                        update book
                        set title = :title,
                            author = :author,
                            publication_city = :publicationCity,
                            publisher = :publisher,
                            imprint = :imprint,
                            year = :year,
                            first_published = :firstPublished,
                            edition = :edition,
                            pages = :pages,
                            frontpages = :frontpages,
                            date_added = :dateAdded,
                            last_audited = :lastAudited,
                            shelf_id = :shelfId,
                            author_secondary = :authorSecondary,
                            isbn = :isbn,
                            special = :special,
                            digital = :digital,
                            hardback = :hardback,
                            dustjacket = :dustjacket,
                            slipcase = :slipcase,
                            video = :video,
                            vols = :vols,
                            series = :series,
                            shelf_coords = :shelfCoords,
                            note = :note,
                            price_paid = :pricePaid,
                            price_on_item = :priceOnItem,
                            price_to_replace = :priceToReplace,
                            price_to_replace_checked = :priceToReplaceChecked,
                            updated_at = now()
                        where id = :id
                        """,
                toParams(book));
        replaceCollections(book.id(), book.collectionIds());
    }

    /**
     * Deletes a book row.
     *
     * @param id unique book identifier
     */
    public void delete(UUID id) {
        jdbcTemplate.update("delete from book where id = :id", Map.of("id", id));
    }

    private void replaceCollections(UUID bookId, List<String> collectionIds) {
        jdbcTemplate.update("delete from book_collection where book_id = :bookId", Map.of("bookId", bookId));
        if (collectionIds == null || collectionIds.isEmpty()) {
            return;
        }
        collectionIds.stream().distinct().forEach(collectionId ->
                jdbcTemplate.update("""
                                insert into book_collection (book_id, collection_id)
                                values (:bookId, :collectionId)
                                """,
                        Map.of("bookId", bookId, "collectionId", collectionId)));
    }

    private MapSqlParameterSource toParams(Book book) {
        return new MapSqlParameterSource()
                .addValue("id", book.id())
                .addValue("title", book.title())
                .addValue("author", book.author())
                .addValue("publicationCity", book.publicationCity())
                .addValue("publisher", book.publisher())
                .addValue("imprint", book.imprint())
                .addValue("year", book.year())
                .addValue("firstPublished", book.firstPublished())
                .addValue("edition", book.edition())
                .addValue("pages", book.pages())
                .addValue("frontpages", book.frontpages())
                .addValue("dateAdded", book.dateAdded())
                .addValue("lastAudited", book.lastAudited())
                .addValue("shelfId", book.shelfId())
                .addValue("authorSecondary", book.authorSecondary())
                .addValue("isbn", book.isbn())
                .addValue("special", book.special())
                .addValue("digital", book.digital())
                .addValue("hardback", book.hardback())
                .addValue("dustjacket", book.dustjacket())
                .addValue("slipcase", book.slipcase())
                .addValue("video", book.video())
                .addValue("vols", book.vols())
                .addValue("series", book.series())
                .addValue("shelfCoords", book.shelfCoords())
                .addValue("note", book.note())
                .addValue("pricePaid", book.pricePaid())
                .addValue("priceOnItem", book.priceOnItem())
                .addValue("priceToReplace", book.priceToReplace())
                .addValue("priceToReplaceChecked", book.priceToReplaceChecked());
    }

    private static class BookRowMapper implements RowMapper<Book> {
        @Override
        public Book mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Book(
                    rs.getObject("id", UUID.class),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("publication_city"),
                    rs.getString("publisher"),
                    rs.getString("imprint"),
                    (Integer) rs.getObject("year"),
                    (Integer) rs.getObject("first_published"),
                    rs.getString("edition"),
                    (Integer) rs.getObject("pages"),
                    rs.getString("frontpages"),
                    rs.getDate("date_added") == null ? null : rs.getDate("date_added").toLocalDate(),
                    rs.getDate("last_audited") == null ? null : rs.getDate("last_audited").toLocalDate(),
                    rs.getString("shelf_id"),
                    rs.getString("author_secondary"),
                    rs.getString("isbn"),
                    rs.getBoolean("special"),
                    rs.getBoolean("digital"),
                    (Boolean) rs.getObject("hardback"),
                    rs.getBoolean("dustjacket"),
                    rs.getBoolean("slipcase"),
                    rs.getBoolean("video"),
                    (Integer) rs.getObject("vols"),
                    rs.getString("series"),
                    rs.getString("shelf_coords"),
                    rs.getString("note"),
                    rs.getBigDecimal("price_paid"),
                    rs.getBigDecimal("price_on_item"),
                    rs.getBigDecimal("price_to_replace"),
                    rs.getDate("price_to_replace_checked") == null ? null : rs.getDate("price_to_replace_checked").toLocalDate(),
                    rs.getObject("created_at", OffsetDateTime.class),
                    rs.getObject("updated_at", OffsetDateTime.class),
                    List.of()
            );
        }
    }
}
