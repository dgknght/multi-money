alter table entities
  drop column if exists first_transaction_date,
  drop column if exists last_transaction_date;

alter table accounts
  drop column if exists first_transaction_date,
  drop column if exists last_transaction_date,
  drop column if exists balance;

alter table transaction_items
  drop column debit_index,
  drop column debit_balance,
  drop column credit_index,
  drop column credit_balance;
