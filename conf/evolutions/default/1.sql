# First evolution for database.

# --- !Ups

create table users (
  user_id bigint not null,
  user_name varchar(64) not null,
  email varchar(255) not null,
  password_hash bigint not null,
  salt bigint not null,
  user_role integer not null,
  deleted boolean not null,
  constraint pk_user primary key (user_id)
);

create unique index users_user_name on users (user_name);

create sequence users_seq;

create table site (
  site_id bigint not null,
  site_name varchar(64) not null,
  created_at timestamp not null
);

alter table site add constraint site_site_name unique (site_name);
alter table site add constraint pk_site primary key (site_id);

create sequence site_seq;

# --- !Downs

drop table site;
drop sequence site_seq;

drop table users;
drop sequence users_seq;
