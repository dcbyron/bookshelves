insert into shelf (id, name) values
    ('uncategorized', 'Uncategorized'),
    ('shelf-a', 'General shelf'),
    ('shelf-b', 'Secondary shelf'),
    ('shelf-c', 'Shelf C'),
    ('shelf-d', 'Shelf D')
on conflict (id) do nothing;

insert into collection (id, name) values
    ('general', 'General'),
    ('secondary', 'Secondary'),
    ('bestsellers', 'Bestsellers'),
    ('language', 'Language'),
    ('excluded', 'Excluded')
on conflict (id) do nothing;
