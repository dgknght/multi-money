create table transactions (
  id uuid DEFAULT gen_random_uuid() NOT NULL,
  entity_id int not null references entities (id) on delete cascade,
  "date" date not null,
  description varchar(100) NOT NULL,
  memo text NULL,
  created_at timestamp with time zone DEFAULT now() NOT NULL,
  updated_at timestamp with time zone DEFAULT now() NOT NULL,
  primary key ("date", id)
) partition by range ("date");
create index ix_transactions_date_entity_id on transactions("date", entity_id);
grant select, insert, update, delete on transactions to app_user;

create table transaction_items (
  id uuid DEFAULT gen_random_uuid() NOT NULL,
  transaction_id uuid not null,
  "date" date not null,
  debit_account_id int not null references accounts (id) on delete cascade,
  credit_account_id int not null references accounts (id) on delete cascade,
  quantity numeric(12, 4) not null,
  created_at timestamp with time zone DEFAULT now() NOT NULL,
  updated_at timestamp with time zone DEFAULT now() NOT NULL,
  primary key ("date", id)
) partition by range ("date");

alter table transaction_items
add constraint fk_transaction_items_transaction_id foreign key ("date", transaction_id) references transactions ("date", id) on delete cascade;

create index ix_transaction_items_date_transaction on transaction_items("date", transaction_id);
create index ix_transaction_items_date_debit_account_id on transaction_items("date", debit_account_id);
create index ix_transaction_items_date_credit_account_id on transaction_items("date", credit_account_id);
grant select, insert, update, delete on transaction_items to app_user;
