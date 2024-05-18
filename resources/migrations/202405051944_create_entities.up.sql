create table entities (
  id serial,
  owner_id int not null references users (id) on delete cascade,
  name varchar(100) NOT NULL,
  created_at timestamp with time zone DEFAULT now() NOT NULL,
  updated_at timestamp with time zone DEFAULT now() NOT NULL,
  primary key (id)
);
create unique index uk_entities_name on entities(owner_id, name);
grant select, insert, update, delete on entities to app_user;
grant select, insert, update, delete on entities_id_seq to app_user;
