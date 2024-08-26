alter table entities
  add column first_transaction_date date,
  add column last_transaction_date date;

alter table accounts
  add column first_transaction_date date,
  add column last_transaction_date date,
  add column balance numeric(12, 4) not null default 0.0;

alter table transaction_items
  add column debit_index bigint not null,
  add column debit_balance numeric(12, 4) not null,
  add column credit_index bigint not null,
  add column credit_balance numeric(12, 4) not null;
