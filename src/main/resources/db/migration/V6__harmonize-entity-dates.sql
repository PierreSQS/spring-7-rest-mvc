-- every entity now has the same date columns as beer and customer:
-- created_date and update_date, both datetime(6)

-- the "last modified" column gets the name update_date
alter table beer_order rename column last_modified_date to update_date;
alter table beer_order_line rename column last_modified_date to update_date;
alter table beer_order_shipment rename column last_modified_date to update_date;
alter table category rename column last_modified_date to update_date;

-- created_date was a TIMESTAMP in these two tables: no microseconds, and limited to the year 2038
alter table beer_order_shipment modify created_date datetime(6);
alter table category modify created_date datetime(6);
