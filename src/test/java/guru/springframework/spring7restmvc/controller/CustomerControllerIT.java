package guru.springframework.spring7restmvc.controller;

import guru.springframework.spring7restmvc.entities.Customer;
import guru.springframework.spring7restmvc.mappers.CustomerMapper;
import guru.springframework.spring7restmvc.model.CustomerDTO;
import guru.springframework.spring7restmvc.repositories.CustomerRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Modified by Pierrot on 29-10-2026
 */
@SpringBootTest
@AutoConfigureMockMvc
class CustomerControllerIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    EntityManager entityManager;

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
        assertThrows(NotFoundException.class, () -> customerController.deleteCustomerById(customerId));
    }

    @Test
    void testUpdateNotFound() {
        UUID customerId = UUID.randomUUID();
        CustomerDTO customerDTO = CustomerDTO.builder().build();
        assertThrows(NotFoundException.class, () ->
                customerController.updateCustomerByID(customerId, customerDTO));
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

        Customer updatedCustomer = customerRepository.findById(customer.getId()).orElseThrow();
        assertThat(updatedCustomer.getName()).isEqualTo(customerName);
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

        Customer customer = customerRepository.findById(savedUUID).orElseThrow();
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
        assertThrows(NotFoundException.class, () -> customerController.getCustomerById(customerID));
    }

    @Test
    void testGetById() {
        Customer customer = customerRepository.findAll().getFirst();
        CustomerDTO customerDTO = customerController.getCustomerById(customer.getId());
        assertThat(customerDTO).isNotNull();
    }

    @Transactional   // rolled back at the end: the bootstrap customer keeps its name for the other tests
    @Test
    void testPatchCustomer() throws Exception {
        Customer customer = customerRepository.findAll().getFirst();

        Map<String, Object> customerMap = new HashMap<>();
        customerMap.put("name", "Patched Name");

        mockMvc.perform(patch(CustomerController.CUSTOMER_PATH_ID, customer.getId())
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(customerMap)))
                .andExpect(status().isNoContent());

        // write to the database, then forget Hibernate's memory, so findById reads the real row (R6)
        entityManager.flush();
        entityManager.clear();

        assertThat(customerRepository.findById(customer.getId()))
                .get()
                .extracting(Customer::getName)
                .isEqualTo("Patched Name");
    }

    // NO @Transactional (R9): the entity is validated when the transaction commits. Inside a test
    // transaction nothing would be committed, @NotBlank would never run and the PATCH would answer 204.
    @Test
    void testPatchCustomerBlankName() throws Exception {
        Customer customer = customerRepository.findAll().getFirst();
        String originalName = customer.getName();

        Map<String, Object> customerMap = new HashMap<>();
        customerMap.put("name", "");

        mockMvc.perform(patch(CustomerController.CUSTOMER_PATH_ID, customer.getId())
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(customerMap)))
                .andExpect(status().isBadRequest());

        assertThat(customerRepository.findById(customer.getId()))
                .get()
                .extracting(Customer::getName)
                .isEqualTo(originalName);
    }

    @Test
    void testPatchCustomerNotFound() throws Exception {
        Map<String, Object> customerMap = new HashMap<>();
        customerMap.put("name", "Patched Name");

        mockMvc.perform(patch(CustomerController.CUSTOMER_PATH_ID, UUID.randomUUID())
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(customerMap)))
                .andExpect(status().isNotFound());
    }

    // the POST request carries its own id and version: the API must ignore both (issue #15, R1)
    @Transactional
    @Test
    void testPostCustomerIgnoresIdAndVersionSentByCustomer() throws Exception {
        // 1. a DTO with a name, a chosen id and a version
        CustomerDTO customerDTO = CustomerDTO.builder()
                .name("New Customer")
                .id(UUID.randomUUID())
                .version(5)
                .build();

        // 2. POST it as JSON, expect 201 Created
        MvcResult mvcResult = mockMvc.perform(post(CustomerController.CUSTOMER_PATH)
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(customerDTO)))
                .andExpect(status().isCreated())
                .andDo(print()) // just for debugging purposes, to see the Location header in the console
                .andReturn();

        // 3. read the id of the new customer from the Location header
        String[] locationParts = Objects.requireNonNull(mvcResult.getResponse()
                .getHeader("Location")).split("/");

        // 4. the new id is not the one sent, and the customer read back has the first version (0)
        UUID savedUUID = UUID.fromString(locationParts[locationParts.length - 1]);
        Customer savedCustomer = customerRepository.findById(savedUUID).orElseThrow();
        assertThat(savedCustomer.getId()).isNotEqualTo(customerDTO.getId());
        assertThat(savedCustomer.getVersion()).isZero();

    }

    // an email of spaces passes the DTO but not the entity: Hibernate refuses it at commit (issue #7)
    // no @Transactional: the service transaction must commit, otherwise Hibernate never validates (R9)
    @Test
    void testPostCustomerBlankEmail() throws Exception {
        // 1. a DTO with a valid name and an email of 3 spaces
        CustomerDTO customerDTO = CustomerDTO.builder()
                .name("New Customer")
                .email("   ")
                .build();

        // 2. POST it as JSON, expect 400 Bad Request, 3. the response body is empty
        mockMvc.perform(post(CustomerController.CUSTOMER_PATH)
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(customerDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));
    }

    // an empty email is refused by the DTO already: the answer names the field (issue #7)
    // only the field is checked, not the message: its text depends on the locale and on @Size
    @Test
    void testPostCustomerEmptyEmail() throws Exception {
        // 1. a DTO with a valid name and an empty email
        CustomerDTO customerDTO = CustomerDTO.builder()
                .name("New Customer")
                .email("")
                .build();

        // 2. POST it as JSON, expect 400 Bad Request
        // 3. the body is a list of {"<field>": "<message>"}: the first entry names the field "email"
        mockMvc.perform(post(CustomerController.CUSTOMER_PATH)
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(customerDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].email").exists());
    }

    // a real email longer than 20 characters must be accepted: the DTO must allow what the column allows (issue #7)
    @Transactional
    @Test
    void testPostCustomerLongEmail() throws Exception {
        // 1. a DTO with a valid name and an email of 25 characters: pierre.dupont@example.com
        CustomerDTO customerDTO = CustomerDTO.builder()
                .name("New Customer")
                .email("pierre.dupont@example.com")
                .build();

        // 2. POST it as JSON, expect 201 Created
        MvcResult mvcResult = mockMvc.perform(post(CustomerController.CUSTOMER_PATH)
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(customerDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        // send the INSERT (Hibernate validates the entity now) and forget Hibernate's memory,
        // so findById reads the real row; @Transactional still rolls everything back (R6, R9)
        entityManager.flush();
        entityManager.clear();

        // 3. the customer read back from the database has this email
        String[] locationParts = Objects.requireNonNull(mvcResult.getResponse()
                .getHeader("Location")).split("/");
        UUID savedUUID = UUID.fromString(locationParts[locationParts.length - 1]);
        Customer savedCustomer = customerRepository.findById(savedUUID).orElseThrow();
        assertThat(savedCustomer.getEmail()).isEqualTo("pierre.dupont@example.com");
    }

    // the email is optional: a customer without one must be accepted (issue #7)
    @Transactional
    @Test
    void testPostCustomerWithoutEmail() throws Exception {
        // 1. a DTO with a valid name and no email at all
        CustomerDTO customerDTO = CustomerDTO.builder()
                .name("New Customer")
                .build();

        // 2. POST it as JSON, expect 201 Created
        MvcResult mvcResult = mockMvc.perform(post(CustomerController.CUSTOMER_PATH)
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(customerDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        // 3. send the INSERT and forget Hibernate's memory (R6, R9)
        entityManager.flush();
        entityManager.clear();

        // 4. the customer read back from the database has no email
        String[] locationParts = Objects.requireNonNull(mvcResult.getResponse()
                .getHeader("Location")).split("/");
        UUID savedUUID = UUID.fromString(locationParts[locationParts.length - 1]);
        Customer savedCustomer = customerRepository.findById(savedUUID).orElseThrow();
        assertThat(savedCustomer.getEmail()).isNull();
    }
}
