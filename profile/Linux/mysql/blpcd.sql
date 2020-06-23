--
-- cd table, artist table, track table
--

create table cd (
	id integer auto_increment not null primary key,
	title varchar(70) not null,
	artist_id integer not null,
	catalogue varchar(30) not null,
	notes varchar(100)
);

create table artist (
	id integer auto_increment not null primary key,
	name varchar(100) not null
);

--
-- union key, combined by more than one column
--
create table track (
	cd_id integer not null,
	track_id integer not null,
	title varchar(70),
	primary key(cd_id, track_id)
);
