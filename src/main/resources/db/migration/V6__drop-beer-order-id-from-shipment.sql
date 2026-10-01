-- Fremdschlüssel fk_beer_order_shipment_beer_order entfernen
alter table beer_order_shipment drop constraint fk_beer_order_shipment_beer_order;

-- Spalte beer_order_id in beer_order_shipment entfernen
alter table beer_order_shipment drop column beer_order_id;