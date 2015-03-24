CREATE TABLE users (
  id          serial primary key,
  email       text,
  crowd_id    integer,
  auth_token  varchar(30),
  is_verified boolean,
  was_invited boolean,
  name        text
);

CREATE TABLE votes (
  id          serial primary key,
  user_id     integer NOT NULL,
  beer_id     integer NOT NULL,
  created_at  timestamp default current_timestamp
);

CREATE TABLE beers (
  id            serial primary key,
  untappd_id    integer,
  crowd_id      integer,
  is_available  boolean default true,
  added_by      integer NOT NULL
);

CREATE TABLE crowds (
  id       serial primary key,
  name     text NOT NULL,
  admin_id integer NOT NULL
);
