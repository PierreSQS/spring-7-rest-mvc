-- every entity now has the same version column as beer and customer: integer
-- (the Java fields are Integer, like the version in BeerDTO and CustomerDTO)
alter table beer_order modify version integer;
alter table beer_order_line modify version integer;
alter table beer_order_shipment modify version integer;
alter table category modify version integer;
