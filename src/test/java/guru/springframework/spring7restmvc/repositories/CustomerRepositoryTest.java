package guru.springframework.spring7restmvc.repositories;

import guru.springframework.spring7restmvc.entities.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Modified by Pierrot on 28-10-2026
 */
@DataJpaTest(showSql = false)
class CustomerRepositoryTest {

    @Autowired
    CustomerRepository customerRepository;

    @Test
    void testSaveCustomer() {
        Customer customer = customerRepository.save(Customer.builder()
                        .name("New Name")
                .build());

        assertThat(customer.getId()).isNotNull();

    }

    @Test
    void testSaveCustomerGetsCreatedAndUpdateDate() {
        // saveAndFlush: Hibernate fills @CreationTimestamp / @UpdateTimestamp when it writes the INSERT
        Customer customer = customerRepository.saveAndFlush(Customer.builder()
                        .name("New Name")
                .build());

        assertThat(customer.getCreatedDate()).isNotNull();
        assertThat(customer.getUpdateDate()).isNotNull();
    }
}