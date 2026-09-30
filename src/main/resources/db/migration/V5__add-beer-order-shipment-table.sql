create table beer_order_shipment (
    id varchar(36) not null,
    beer_order_id varchar(36) unique,
    tracking_number varchar(50),
    created_date datetime(6),
    update_date datetime(6),
    version integer,
    primary key (id),
    constraint fk_beer_order_shipment_beer_order
        foreign key (beer_order_id) references beer_order (id)
) engine=InnoDB;

alter table beer_order
    add column beer_order_shipment_id varchar(36);

alter table beer_order
    add constraint fk_beer_order_shipment
        foreign key (beer_order_shipment_id) references beer_order_shipment (id);
