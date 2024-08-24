alter table entities
  drop column if exists first_transaction_date,
  drop column if exists last_transaction_date;

alter table accounts
  drop column if exists first_transaction_date,
  drop column if exists last_transaction_date,
  drop column if exists quantity;
