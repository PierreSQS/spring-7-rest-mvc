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
 * Modified by Pierrot on 19-10-2026
 */
@Service
@Primary
@RequiredArgsConstructor
public class CustomerServiceJPA implements CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    public Optional<CustomerDTO> getCustomerById(UUID uuid) {
        return customerRepository.findById(uuid).map(customerMapper::customerToCustomerDto);
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
    public boolean deleteCustomerById(UUID customerId) {
        return customerRepository.findById(customerId).map(foundCustomer -> {
            customerRepository.delete(foundCustomer);
            return true;
        }).orElse(false);
    }

    @Override
    public Optional<CustomerDTO> patchCustomerById(UUID customerId, CustomerDTO customerDTO) {

        return customerRepository.findById(customerId).map(foundCustomer -> {

            customerMapper.patchCustomerFromDto(customerDTO, foundCustomer);

            return customerMapper.customerToCustomerDto(customerRepository.save(foundCustomer));
        });
    }
}
