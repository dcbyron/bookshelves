create extension if not exists "uuid-ossp";

create table if not exists collection (
    id text primary key,
    name text not null
);

create table if not exists shelf (
    id text primary key,
    name text
);

create table if not exists book (
    id uuid primary key default uuid_generate_v4(),
    title text not null,
    author text,
    publication text,
    year integer,
    edition text,
    pages integer,
    frontpages text,
    date_added date,
    shelf_id text references shelf (id) on update cascade on delete cascade,
    author_secondary text,
    isbn text,
    special boolean default false,
    hardback boolean,
    vols integer default 1,
    series text,
    shelf_coords text,
    note text,
    price_paid numeric(10, 5),
    price_on_item numeric(10, 5),
    price_to_replace numeric(10, 5),
    price_to_replace_checked date,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists book_collection (
    id uuid primary key default uuid_generate_v4(),
    book_id uuid not null references book (id) on update cascade on delete cascade,
    collection_id text not null references collection (id) on update cascade on delete cascade
);

create unique index if not exists ux_book_collection_pair on book_collection (book_id, collection_id);
create index if not exists idx_book_title on book (title);
create index if not exists idx_book_author on book (author);
create index if not exists idx_book_year on book (year);
create index if not exists idx_book_shelf_id on book (shelf_id);
create index if not exists idx_book_collection_book_id on book_collection (book_id);
create index if not exists idx_book_collection_collection_id on book_collection (collection_id);
