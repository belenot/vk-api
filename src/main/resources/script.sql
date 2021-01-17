create table if not exists scrape_users (
    user_id int unique
);

create table if not exists users_online (
    id serial not null,
    user_id int,
    first_name varchar(256),
    last_name varchar(256),
    is_online bool,
    scrape_time timestamp
);