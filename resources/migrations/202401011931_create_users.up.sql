create table users (
  id serial,
  email varchar(100) NOT NULL,
  given_name varchar(100) NOT NULL,
  surname varchar(100) NOT NULL,
  created_at timestamp with time zone DEFAULT now() NOT NULL,
  updated_at timestamp with time zone DEFAULT now() NOT NULL,
  primary key (id)
);
create unique index uk_users_email on users(email);
grant select, insert, update, delete on users to app_user;
grant select, insert, update, delete on users_id_seq to app_user;

create table identities (
    id serial,
    user_id int not null references users (id) on delete cascade,
    oauth_provider character varying(20) NOT NULL,
    oauth_id character varying(255) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    primary key (id)
);
create unique index uk_identities_provider_id on identities(oauth_id, oauth_provider);
create unique index uk_identities_user_id on identities(user_id, oauth_provider);
grant select, insert, update, delete on identities to app_user;
grant select, insert, update, delete on identities_id_seq to app_user;
