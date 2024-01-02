create table users (
  id serial,
  username varchar(100) not null,
  email varchar(100) NOT NULL,
  given_name varchar(100) NOT NULL,
  surname varchar(100) NOT NULL,
  created_at timestamp with time zone DEFAULT now() NOT NULL,
  updated_at timestamp with time zone DEFAULT now() NOT NULL,
  primary key (id)
);
create unique index uk_users_email on users(email);
create unique index uk_users_username on users(username);

create table identities (
    id serial,
    user_id int not null references users (id) on delete cascade,
    provider character varying(20) NOT NULL,
    provider_id character varying(255) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    primary key (id)
);
create unique index uk_identities_provider_id on identities(provider_id, provider);
