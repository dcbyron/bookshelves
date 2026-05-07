alter table book
    add column dustjacket boolean not null default false,
    add column slipcase boolean not null default false,
    add column video boolean not null default false;
