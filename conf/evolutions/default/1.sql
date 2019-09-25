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
  held_on_utc timestamp not null,
  held_on_zone_id varchar(64) not null,
  owner bigint not null,
  created_at timestamp not null
);

alter table site add constraint site_site_name unique (site_name);
alter table site add constraint pk_site primary key (site_id);
alter table site add constraint site_owner_fkey foreign key (owner) references users(user_id) on delete cascade;

create sequence site_seq;

create table agent_record (
  agent_record_id bigint not null,
  site_id bigint not null,
  agent_name varchar(256),
  agent_level integer not null,
  lifetime_ap bigint not null,
  distance_walked integer not null,
  phase integer not null,
  tsv text not null,
  created_at timestamp not null
);

alter table agent_record add constraint pk_agent_record primary key (agent_record_id);
alter table agent_record
  add constraint agent_record_site_id_fkey foreign key (site_id) references site(site_id) on delete cascade;
alter table agent_record add constraint agent_record_agent_name_phase unique (site_id, agent_name, phase);
create index ix_agent_record0 on agent_record (site_id);
create index ix_agent_record1 on agent_record (agent_name);

create sequence agent_record_seq;

# --- !Downs

drop table agent_record;
drop sequence agent_record_seq;

drop table site;
drop sequence site_seq;

drop table users;
drop sequence users_seq;
