package guru.springframework.spring7restmvc.services;

import guru.springframework.spring7restmvc.mappers.CustomerMapper;
import guru.springframework.spring7restmvc.model.CustomerDTO;
import guru.springframework.spring7restmvc.repositories.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 17-10-2026
 */
@Service
@Primary
@RequiredArgsConstructor
public class CustomerServiceJPA implements CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    public Optional<CustomerDTO> getCustomerById(UUID uuid) {
        return Optional.ofNullable(customerMapper
                .customerToCustomerDto(customerRepository.findById(uuid).orElse(null)));
    }

    @Override
    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(customerMapper::customerToCustomerDto)
                .toList();
    }

    @Override
    public CustomerDTO saveNewCustomer(CustomerDTO customerDTO) {
        return customerMapper.customerToCustomerDto(customerRepository
                .save(customerMapper.customerDtoToCustomer(customerDTO)));
    }

    @Override
    public Optional<CustomerDTO> updateCustomerById(UUID customerId, CustomerDTO customerDTO) {

        return customerRepository.findById(customerId).map(foundCustomer -> {
            customerMapper.updateCustomerFromDto(customerDTO, foundCustomer);

            return customerMapper.customerToCustomerDto(customerRepository.save(foundCustomer));
        });
    }

    @Override
    public Boolean deleteCustomerById(UUID customerId) {
        if(customerRepository.existsById(customerId)){
            customerRepository.deleteById(customerId);
            return true;
        }
        return false;
    }

    @Override
    public Optional<CustomerDTO> patchCustomerById(UUID customerId, CustomerDTO customerDTO) {

        return customerRepository.findById(customerId).map(foundCustomer -> {

            customerMapper.patchCustomerFromDto(customerDTO, foundCustomer);

            return customerMapper.customerToCustomerDto(customerRepository.save(foundCustomer));
        });
    }
}
