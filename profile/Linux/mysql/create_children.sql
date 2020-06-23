--
--	create table
--
create table children (
	childno int(11) NOT NULL auto_increment,
	name varchar(30),
	age int(11),
	primary key (childno)
);

--
-- Populate table
--

insert into children(childno, name, age) values(1, 'Jenny', 21);
insert into children(childno, name, age) values(2, 'Andrew', 17);
insert into children(childno, name, age) values(3, 'Gavin', 8);
insert into children(childno, name, age) values(4, 'Duncan', 6);
insert into children(childno, name, age) values(5, 'Emma', 4);
insert into children(childno, name, age) values(6, 'Alex', 15);
insert into children(childno, name, age) values(7, 'Adrian', 9);
