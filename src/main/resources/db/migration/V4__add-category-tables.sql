create table category (
    id varchar(36) not null,
    created_date datetime(6),
    description varchar(50),
    update_date datetime(6),
    version integer,
    primary key (id)
) engine=InnoDB;

create table beer_category (
    beer_id varchar(36) not null,
    category_id varchar(36) not null,
    primary key (beer_id, category_id),
    constraint fk_beer_category_beer
        foreign key (beer_id) references beer (id),
    constraint fk_beer_category_category
        foreign key (category_id) references category (id)
) engine=InnoDB;
