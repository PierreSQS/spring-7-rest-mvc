package guru.springframework.spring7restmvc.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 25-09-2026
 */
@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
public class BeerOrder {

    // written by hand instead of @AllArgsConstructor: the builder calls this constructor, and it
    // must go through setCustomer(...) so the customer's order list gets the order too
    public BeerOrder(UUID id, Integer version, LocalDateTime createdDate, LocalDateTime updateDate,
                     String customerRef, Customer customer, BeerOrderShipment beerOrderShipment, Set<BeerOrderLine> beerOrderLines) {
        this.id = id;
        this.version = version;
        this.createdDate = createdDate;
        this.updateDate = updateDate;
        this.customerRef = customerRef;
        this.setCustomer(customer);
        this.setBeerOrderShipment(beerOrderShipment);
        this.beerOrderLines = beerOrderLines;
    }

    @Id
    @UuidGenerator
    @Column(length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Version
    private Integer version;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updateDate;

    private String customerRef;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "beer_order_shipment_id")
    private BeerOrderShipment beerOrderShipment;

    @OneToMany(mappedBy = "beerOrder")
    private Set<BeerOrderLine> beerOrderLines;

    public void setCustomer(Customer customer) {
        this.customer = customer;
        if (customer != null) {
            customer.getBeerOrders().add(this);
        }
    }

    public void setBeerOrderShipment(BeerOrderShipment beerOrderShipment) {
        this.beerOrderShipment = beerOrderShipment;
        if (beerOrderShipment != null) {
            beerOrderShipment.setBeerOrder(this);
        }
    }

}
