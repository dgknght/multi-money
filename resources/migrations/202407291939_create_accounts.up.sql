create table accounts (
  id serial,
  entity_id int not null references entities (id) on delete cascade,
  commodity_id int not null references commodities (id),
  name varchar(100) NOT NULL,
  type varchar(10) NOT NULL,
  created_at timestamp with time zone DEFAULT now() NOT NULL,
  updated_at timestamp with time zone DEFAULT now() NOT NULL,
  primary key (id)
);
create unique index uk_accountss_name on accounts(entity_id, name);
grant select, insert, update, delete on accounts to app_user;
grant select, insert, update, delete on accounts_id_seq to app_user;
