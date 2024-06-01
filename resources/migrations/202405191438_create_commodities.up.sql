create table commodities (
  id serial,
  entity_id int not null references entities (id) on delete cascade,
  symbol varchar(10) NOT NULL,
  name varchar(100) NOT NULL,
  type varchar(20) NOT NULL,
  created_at timestamp with time zone DEFAULT now() NOT NULL,
  updated_at timestamp with time zone DEFAULT now() NOT NULL,
  primary key (id)
);
create unique index uk_commodities_symbol on commodities(entity_id, symbol);
grant select, insert, update, delete on commodities to app_user;
grant select, insert, update, delete on commodities_id_seq to app_user;
