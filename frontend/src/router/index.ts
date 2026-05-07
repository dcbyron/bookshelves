import { createRouter, createWebHistory } from "vue-router";

import BooksView from "../views/BooksView.vue";
import BookEditorView from "../views/BookEditorView.vue";
import CollectionsView from "../views/CollectionsView.vue";
import ShelvesView from "../views/ShelvesView.vue";
import BackupView from "../views/BackupView.vue";
import AuthorsView from "../views/AuthorsView.vue";
import ReportsView from "../views/ReportsView.vue";
import ValueAnalyticsView from "../views/ValueAnalyticsView.vue";
import MissingLocationView from "../views/MissingLocationView.vue";
import PublishersView from "../views/PublishersView.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/books" },
    { path: "/books", name: "books", component: BooksView, meta: { title: "Books" } },
    { path: "/books/new", name: "book-new", component: BookEditorView, meta: { title: "New Book" } },
    { path: "/books/:id", name: "book-edit", component: BookEditorView, props: true, meta: { title: "Edit Book" } },
    { path: "/authors", name: "authors", component: AuthorsView, meta: { title: "Authors" } },
    { path: "/value", name: "value", component: ValueAnalyticsView, meta: { title: "Value" } },
    { path: "/recently-added", redirect: "/reports" },
    { path: "/recently-updated", redirect: "/reports" },
    { path: "/reports/missing-location", redirect: "/missing-location" },
    { path: "/reports", name: "reports", component: ReportsView, meta: { title: "Reports" } },
    { path: "/missing-location", name: "missing-location", component: MissingLocationView, meta: { title: "Missing Location" } },
    { path: "/publishers", name: "publishers", component: PublishersView, meta: { title: "Publishers" } },
    { path: "/collections", name: "collections", component: CollectionsView, meta: { title: "Collections" } },
    { path: "/shelves", name: "shelves", component: ShelvesView, meta: { title: "Shelves" } },
    { path: "/backup", name: "backup", component: BackupView, meta: { title: "Backup" } }
  ]
});

export default router;
