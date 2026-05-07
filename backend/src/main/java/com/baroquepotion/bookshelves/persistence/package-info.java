/**
 * JDBC-backed repositories for the catalog database.
 *
 * <p>Repositories in this package own SQL access to the Flyway-managed PostgreSQL schema and keep
 * persistence details out of the application and API layers. Alongside core CRUD and paginated
 * search, this package now also provides the browse/report query surface used by author browsing,
 * recent-activity views, and grouped reporting.
 */
package com.baroquepotion.bookshelves.persistence;
