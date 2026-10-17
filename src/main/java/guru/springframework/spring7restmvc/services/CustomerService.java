package guru.springframework.spring7restmvc.services;

import guru.springframework.spring7restmvc.model.CustomerDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 17-10-2026
 */
public interface CustomerService {

    Optional<CustomerDTO> getCustomerById(UUID uuid);

    List<CustomerDTO> getAllCustomers();

    CustomerDTO saveNewCustomer(CustomerDTO customerDTO);

    Optional<CustomerDTO> updateCustomerById(UUID customerId, CustomerDTO customerDTO);

    Boolean deleteCustomerById(UUID customerId);

    Optional<CustomerDTO> patchCustomerById(UUID customerId, CustomerDTO customerDTO);
}
