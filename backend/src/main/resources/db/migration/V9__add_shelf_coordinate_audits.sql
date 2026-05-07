create table shelf_coordinate_audit (
    shelf_id text not null references shelf (id) on update cascade on delete cascade,
    shelf_coords text not null,
    audited boolean not null default false,
    primary key (shelf_id, shelf_coords)
);
