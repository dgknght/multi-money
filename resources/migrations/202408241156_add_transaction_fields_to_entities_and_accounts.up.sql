alter table entities
  add column first_transaction_date date,
  add column last_transaction_date date;

alter table accounts
  add column first_transaction_date date,
  add column last_transaction_date date,
  add column quantity numeric(12, 4) not null default 0.0;
