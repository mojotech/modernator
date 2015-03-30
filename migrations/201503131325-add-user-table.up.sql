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
  crowd_id    integer NOT NULL,
  user_id     integer NOT NULL,
  item_id     integer NOT NULL,
  created_at  timestamp default current_timestamp
);

CREATE TABLE items (
  id            serial primary key,
  title         text,
  crowd_id      integer,
  added_by      integer NOT NULL
);

CREATE TABLE crowds (
  id       serial primary key,
  name     text NOT NULL,
  admin_id integer NOT NULL
);
