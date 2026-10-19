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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every test sends a real HTTP request through MockMvc, so it also goes through the security
 * filter chain, @Validated, the CustomErrorController and the JSON conversion - a direct call to
 * the controller method would skip all of them.
 * Tests that change data are @Transactional (rolled back); before reading back what a request
 * wrote, they flush and clear, so the check reads the database and not Hibernate's cache.
 * <p>
 * Modified by Pierrot on 19-10-2026
 */
@SpringBootTest
@AutoConfigureMockMvc
class CustomerControllerIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    CustomerMapper customerMapper;

    @Autowired
    EntityManager entityManager;

    @Test
    void testRequestWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get(CustomerController.CUSTOMER_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testListAll() throws Exception {
        mockMvc.perform(get(CustomerController.CUSTOMER_PATH)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(3)));
    }

    @Transactional
    @Test
    void testListAllEmpty() throws Exception {
        customerRepository.deleteAll();

        mockMvc.perform(get(CustomerController.CUSTOMER_PATH)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void testGetById() throws Exception {
        Customer customer = customerRepository.findAll().getFirst();

        mockMvc.perform(get(CustomerController.CUSTOMER_PATH_ID, customer.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(customer.getId().toString())))
                .andExpect(jsonPath("$.name", is(customer.getName())));
    }

    @Test
    void testGetByIdNotFound() throws Exception {
        mockMvc.perform(get(CustomerController.CUSTOMER_PATH_ID, UUID.randomUUID())
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Transactional
    @Test
    void testCreate() throws Exception {
        CustomerDTO customerDTO = CustomerDTO.builder()
                .name("TEST")
                .build();

        MvcResult result = mockMvc.perform(post(CustomerController.CUSTOMER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(customerDTO))
                        .with(jwt()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        entityManager.flush();
        entityManager.clear();

        assertThat(customerRepository.findById(idFromLocation(result)))
                .get()
                .extracting(Customer::getName)
                .isEqualTo("TEST");
    }

    // a customer created through the API must get its dates from Hibernate
    // (@CreationTimestamp / @UpdateTimestamp on Customer) - before, only BootstrapData set them by hand
    @Transactional
    @Test
    void testCreateGetsCreatedAndUpdateDate() throws Exception {
        CustomerDTO customerDTO = CustomerDTO.builder()
                .name("TEST")
                .build();

        MvcResult result = mockMvc.perform(post(CustomerController.CUSTOMER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(customerDTO))
                        .with(jwt()))
                .andExpect(status().isCreated())
                .andReturn();

        // Hibernate fills @UpdateTimestamp only when it really writes the row (the flush)
        entityManager.flush();
        entityManager.clear();

        Customer customer = customerRepository.findById(idFromLocation(result)).orElseThrow();

        // the request sent no dates: both must have been filled by Hibernate
        assertThat(customer.getCreatedDate()).isNotNull();
        assertThat(customer.getUpdateDate()).isNotNull();
    }

    @Transactional
    @Test
    void testUpdateById() throws Exception {
        Customer customer = customerRepository.findAll().getFirst();
        CustomerDTO customerDTO = customerMapper.customerToCustomerDto(customer);
        customerDTO.setId(null);
        customerDTO.setVersion(null);
        customerDTO.setName("UPDATED");

        mockMvc.perform(put(CustomerController.CUSTOMER_PATH_ID, customer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(customerDTO))
                        .with(jwt()))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        assertThat(customerRepository.findById(customer.getId()))
                .get()
                .extracting(Customer::getName)
                .isEqualTo("UPDATED");
    }

    @Test
    void testUpdateByIdNotFound() throws Exception {
        CustomerDTO customerDTO = CustomerDTO.builder()
                .name("UPDATED")
                .build();

        mockMvc.perform(put(CustomerController.CUSTOMER_PATH_ID, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(customerDTO))
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    // JT's hasText check ignored an empty name; the MapStruct patch copies it, and without
    // @NotBlank on Customer.name the customer silently lost its name (known bug of ebab5ee8).
    // Not @Transactional on purpose: the save must really commit, because the validation
    // runs when Hibernate writes the change - inside a rolled-back test transaction it never would.
    @Test
    void testPatchBlankNameIsRejected() throws Exception {
        Customer customer = customerRepository.findAll().getFirst();

        mockMvc.perform(patch(CustomerController.CUSTOMER_PATH_ID, customer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}")
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                // the field is named; its message depends on the JVM locale, so it is not compared
                .andExpect(jsonPath("$[0].name").exists());

        // reload the customer: the refused change was rolled back, so the name is still there
        assertThat(customerRepository.findById(customer.getId()))
                .get()
                .extracting(Customer::getName)
                .isEqualTo(customer.getName());
    }

    @Transactional
    @Test
    void testDeleteById() throws Exception {
        Customer customer = customerRepository.findAll().getFirst();

        mockMvc.perform(delete(CustomerController.CUSTOMER_PATH_ID, customer.getId())
                        .with(jwt()))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        assertThat(customerRepository.findById(customer.getId())).isEmpty();
    }

    @Test
    void testDeleteByIdNotFound() throws Exception {
        mockMvc.perform(delete(CustomerController.CUSTOMER_PATH_ID, UUID.randomUUID())
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    /**
     * Reads the id of the created customer from the end of the Location header,
     * e.g. {@code /api/v1/customer/<uuid>}.
     */
    private UUID idFromLocation(MvcResult result) {
        String location = Objects.requireNonNull(result.getResponse().getHeader("Location"),
                "the response has no Location header");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }
}
