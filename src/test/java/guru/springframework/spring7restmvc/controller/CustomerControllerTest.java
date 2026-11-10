package guru.springframework.spring7restmvc.controller;

import guru.springframework.spring7restmvc.config.SpringSecConfig;
import guru.springframework.spring7restmvc.model.CustomerDTO;
import guru.springframework.spring7restmvc.services.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Modified by Pierrot on 10-11-2026
 */
@WebMvcTest(CustomerController.class)
@Import(SpringSecConfig.class)
@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @MockitoBean
    CustomerService customerService;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    CustomerDTO customerDTO1, customerDTO2, customerDTO3;

    @BeforeEach
    void setUp() {
        customerDTO1 = CustomerDTO.builder()
                .id(UUID.randomUUID())
                .name("Customer 1")
                .version(1)
                .createdDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();

        customerDTO2 = CustomerDTO.builder()
                .id(UUID.randomUUID())
                .name("Customer 2")
                .version(1)
                .createdDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();

        customerDTO3 = CustomerDTO.builder()
                .id(UUID.randomUUID())
                .name("Customer 3")
                .version(1)
                .createdDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();
    }

    @Captor
    ArgumentCaptor<UUID> uuidArgumentCaptor;

    @Captor
    ArgumentCaptor<CustomerDTO> customerArgumentCaptor;

    @Test
    void testPatchCustomer() throws Exception {
        Map<String, Object> customerMap = new HashMap<>();
        customerMap.put("name", "New Name");

        given(customerService.patchCustomerById(any(UUID.class), any(CustomerDTO.class)))
                .willReturn(Optional.of(customerDTO1));

        mockMvc.perform(patch( CustomerController.CUSTOMER_PATH_ID, customerDTO1.getId())
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(customerMap)))
                .andExpect(status().isNoContent());

        verify(customerService).patchCustomerById(uuidArgumentCaptor.capture(),
                customerArgumentCaptor.capture());

        assertThat(uuidArgumentCaptor.getValue()).isEqualTo(customerDTO1.getId());
        assertThat(customerArgumentCaptor.getValue().getName())
                .isEqualTo(customerMap.get("name"));
    }

    @Test
    void testDeleteCustomer() throws Exception {
        given(customerService.deleteCustomerById(any())).willReturn(true);

        mockMvc.perform(delete(CustomerController.CUSTOMER_PATH_ID, customerDTO1.getId())
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(customerService).deleteCustomerById(uuidArgumentCaptor.capture());

        assertThat(uuidArgumentCaptor.getValue()).isEqualTo(customerDTO1.getId());
    }

    @Test
    void testUpdateCustomer() throws Exception {
        given(customerService.updateCustomerById(any(), any())).willReturn(Optional.of(customerDTO1));

        mockMvc.perform(put(CustomerController.CUSTOMER_PATH_ID, customerDTO1.getId())
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                .content(jsonMapper.writeValueAsString(customerRequest("Updated Customer")))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(customerService).updateCustomerById(uuidArgumentCaptor.capture(), any(CustomerDTO.class));

        assertThat(uuidArgumentCaptor.getValue()).isEqualTo(customerDTO1.getId());
    }

    @Test
    void testCreateCustomer() throws Exception {
        given(customerService.saveNewCustomer(any(CustomerDTO.class)))
                .willReturn(customerDTO2);

        mockMvc.perform(post(CustomerController.CUSTOMER_PATH).contentType(MediaType.APPLICATION_JSON)
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                .accept(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(customerRequest("New Customer"))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    @Test
    void listAllCustomers() throws Exception {
        given(customerService.getAllCustomers()).willReturn(List.of(customerDTO1, customerDTO2, customerDTO3));

        mockMvc.perform(get(CustomerController.CUSTOMER_PATH)
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()", is(3)));
    }

    @Test
    void getCustomerByIdNotFound() throws Exception {
        given(customerService.getCustomerById(any(UUID.class))).willReturn(Optional.empty());

        mockMvc.perform(get(CustomerController.CUSTOMER_PATH_ID, UUID.randomUUID())
                        .with(BeerControllerTest.jwtRequestPostProcessor))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCustomerById() throws Exception {
        given(customerService.getCustomerById(customerDTO1.getId())).willReturn(Optional.of(customerDTO1));

        mockMvc.perform(get(CustomerController.CUSTOMER_PATH_ID, customerDTO1.getId())
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is(customerDTO1.getName())));
    }

    /**
     * The body a real caller sends (#53): no id, no version, no dates - the database and
     * Hibernate own those. The name is a parameter, like beerRequest in BeerControllerTest.
     */
    private CustomerDTO customerRequest(String name) {
        return CustomerDTO.builder()
                .name(name)
                .email("test@example.com")
                .build();
    }
}










