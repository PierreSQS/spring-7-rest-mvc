package guru.springframework.spring7restmvc.controller;

import guru.springframework.spring7restmvc.entities.Customer;
import guru.springframework.spring7restmvc.model.CustomerDTO;
import guru.springframework.spring7restmvc.repositories.CustomerRepository;
import guru.springframework.spring7restmvc.mappers.CustomerMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Modified by Pierrot on 17-10-2026
 */
@SpringBootTest
class CustomerControllerIT {

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    CustomerController customerController;

    @Autowired
    CustomerMapper customerMapper;

    @Transactional
    @Test
    void deleteByIdFound() {
        Customer customer = customerRepository.findAll().getFirst();

        ResponseEntity<Void> responseEntity = customerController.deleteCustomerById(customer.getId());
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));

        assertThat(customerRepository.findById(customer.getId())).isEmpty();
    }

    @Test
    void testDeleteNotFound() {
        UUID customerId = UUID.randomUUID();

        assertThatThrownBy(() -> customerController.deleteCustomerById(customerId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testUpdateNotFound() {
        UUID customerId = UUID.randomUUID();
        CustomerDTO customerDTO = CustomerDTO.builder().build();

        assertThatThrownBy(() -> customerController.updateCustomerByID(customerId, customerDTO))
                .isInstanceOf(NotFoundException.class);
    }

    @Transactional
    @Test
    void updateExistingBeer() {
        Customer customer = customerRepository.findAll().getFirst();
        CustomerDTO customerDTO = customerMapper.customerToCustomerDto(customer);
        customerDTO.setId(null);
        customerDTO.setVersion(null);
        final String customerName = "UPDATED";
        customerDTO.setName(customerName);

        ResponseEntity<Void> responseEntity = customerController.updateCustomerByID(customer.getId(), customerDTO);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));

        Customer updatedCustomer = customerRepository.findById(customer.getId()).get();
        assertThat(updatedCustomer.getName()).isEqualTo(customerName);
    }

    // JT's hasText check ignored an empty name; the MapStruct patch copies it, and without
    // @NotBlank on Customer.name the customer silently lost its name (known bug of ebab5ee8).
    // Not @Transactional on purpose: the save must really commit, because the validation
    // runs when Hibernate writes the change - inside a rolled-back test transaction it never would.
    @Test
    void testPatchCustomerBlankNameIsRejected() {
        Customer customer = customerRepository.findAll().getFirst();
        UUID customerId = customer.getId();
        CustomerDTO customerDTO = CustomerDTO.builder().name("").build();

        // the save is refused: Bean Validation (@NotBlank) fails while committing
        assertThatThrownBy(() -> customerController.patchCustomerById(customerId, customerDTO))
                .isInstanceOf(TransactionSystemException.class)
                .rootCause()
                .hasMessageContaining("name");

        // reload the customer: the refused change was rolled back, so the name is still there
        assertThat(customerRepository.findById(customerId))
                .get()
                .extracting(Customer::getName)
                .isEqualTo(customer.getName());
    }

    @Transactional
    @Test
    void saveNewBeerTest() {
       CustomerDTO customerDTO = CustomerDTO.builder()
               .name("TEST")
               .build();

        ResponseEntity<Void> responseEntity = customerController.handlePost(customerDTO);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(201));
        assertThat(responseEntity.getHeaders().getLocation()).isNotNull();

        String[] locationUUID = responseEntity.getHeaders().getLocation().getPath().split("/");
        UUID savedUUID = UUID.fromString(locationUUID[4]);

        Customer customer = customerRepository.findById(savedUUID).get();
        assertThat(customer).isNotNull();
    }

    @Transactional
    @Test
    void testListAllEmptyList() {
        customerRepository.deleteAll();
        List<CustomerDTO> dtos = customerController.listAllCustomers();

        assertThat(dtos).isEmpty();
    }

    @Test
    void testListAll() {
        List<CustomerDTO> dtos = customerController.listAllCustomers();

        assertThat(dtos).hasSize(3);
    }

    @Test
    void testGetByIdNotFound() {
        UUID customerID = UUID.randomUUID();

        assertThatThrownBy(() -> customerController.getCustomerById(customerID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testGetById() {
        Customer customer = customerRepository.findAll().getFirst();
        CustomerDTO customerDTO = customerController.getCustomerById(customer.getId());
        assertThat(customerDTO).isNotNull();
    }
}
