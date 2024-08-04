create table accounts (
  id serial,
  entity_id int not null references entities (id) on delete cascade,
  commodity_id int not null references commodities (id),
  parent_id int null references accounts (id),
  name varchar(100) NOT NULL,
  type varchar(10) NOT NULL,
  created_at timestamp with time zone DEFAULT now() NOT NULL,
  updated_at timestamp with time zone DEFAULT now() NOT NULL,
  primary key (id)
);
create unique index uk_accounts_name on accounts(entity_id, name);
create index ix_accounts_commodity_id on accounts(commodity_id);
grant select, insert, update, delete on accounts to app_user;
grant select, insert, update, delete on accounts_id_seq to app_user;
