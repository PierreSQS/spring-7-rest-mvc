package guru.springframework.spring7restmvc.controller;

import guru.springframework.spring7restmvc.entities.Customer;
import guru.springframework.spring7restmvc.mappers.CustomerMapper;
import guru.springframework.spring7restmvc.model.CustomerDTO;
import guru.springframework.spring7restmvc.repositories.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Modified by Pierrot on 22-09-2026
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
        Customer customer = getFirstCustomer();

        ResponseEntity<Void> responseEntity = customerController.deleteCustomerById(customer.getId());
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

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
    void updateExistingCustomer() {
        Customer customer = getFirstCustomer();
        CustomerDTO customerDTO = customerMapper.customerToCustomerDto(customer);
        final String customerName = "UPDATED";
        final String customerEmail = "updated@example.com";
        customerDTO.setName(customerName);
        customerDTO.setEmail(customerEmail);

        ResponseEntity<Void> responseEntity = customerController.updateCustomerByID(customer.getId(), customerDTO);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Customer updatedCustomer = customerRepository.findById(customer.getId()).orElseThrow();
        assertThat(updatedCustomer.getName()).isEqualTo(customerName);
        assertThat(updatedCustomer.getEmail()).isEqualTo(customerEmail);
    }

    @Test
    void testPatchNotFound() {
        UUID customerId = UUID.randomUUID();
        CustomerDTO customerDTO = CustomerDTO.builder().build();

        assertThatThrownBy(() -> customerController.patchCustomerById(customerId, customerDTO))
                .isInstanceOf(NotFoundException.class);
    }

    @Transactional
    @Test
    void patchExistingCustomerEmail() {
        Customer customer = getFirstCustomer();
        String originalName = customer.getName();
        final String customerEmail = "patched@example.com";
        CustomerDTO customerDTO = CustomerDTO.builder().email(customerEmail).build();

        ResponseEntity<Void> responseEntity = customerController.patchCustomerById(customer.getId(), customerDTO);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // PATCH only touches the fields it carries: the email changes, the name stays
        Customer patchedCustomer = customerRepository.findById(customer.getId()).orElseThrow();
        assertThat(patchedCustomer.getEmail()).isEqualTo(customerEmail);
        assertThat(patchedCustomer.getName()).isEqualTo(originalName);
    }

    @Transactional
    @Test
    void patchLeavesTheIdAndTheCreatedDateAlone() {
        Customer customer = getFirstCustomer();
        LocalDateTime createdDate = customer.getCreatedDate();
        CustomerDTO customerDTO = CustomerDTO.builder()
                .id(UUID.randomUUID())
                .createdDate(LocalDateTime.now().minusYears(1))
                .name("PATCHED")
                .build();

        customerController.patchCustomerById(customer.getId(), customerDTO);

        // the mapper ignores id, version and the timestamps, so a request body cannot overwrite them
        Customer patchedCustomer = customerRepository.findById(customer.getId()).orElseThrow();
        assertThat(patchedCustomer.getName()).isEqualTo("PATCHED");
        assertThat(patchedCustomer.getCreatedDate()).isEqualTo(createdDate);
        assertThat(customerRepository.findById(customerDTO.getId())).isEmpty();
    }

    @Transactional
    @Test
    void saveNewCustomerTest() {
        CustomerDTO customerDTO = CustomerDTO.builder()
                .name("TEST")
                .build();

        ResponseEntity<Void> responseEntity = customerController.handlePost(customerDTO);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        URI location = responseEntity.getHeaders().getLocation();
        assertThat(location).isNotNull();

        UUID savedUUID = UUID.fromString(
                location.getPath().substring(CustomerController.CUSTOMER_PATH.length() + 1));

        assertThat(customerRepository.findById(savedUUID)).isPresent();
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
        UUID customerId = UUID.randomUUID();

        assertThatThrownBy(() -> customerController.getCustomerById(customerId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testGetById() {
        Customer customer = getFirstCustomer();

        CustomerDTO customerDTO = customerController.getCustomerById(customer.getId());

        assertThat(customerDTO.getId()).isEqualTo(customer.getId());
    }

    private Customer getFirstCustomer() {
        return customerRepository.findAll().getFirst();
    }
}
