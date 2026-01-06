package com.roshansutihar.bankingservice.service;

import com.roshansutihar.bankingservice.entity.Business;
import com.roshansutihar.bankingservice.repository.BusinessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BusinessService {

    @Autowired
    private BusinessRepository businessRepository;

    public List<Business> getAllBusinesses() {
        return businessRepository.findAll();
    }

    public Optional<Business> getBusinessById(Long id) {
        return businessRepository.findById(id);
    }

    public Optional<Business> getBusinessByUserId(Long userId) {
        return businessRepository.findByUserId(userId);
    }

    public Optional<Business> getBusinessByTaxId(String taxId) {
        return businessRepository.findByTaxId(taxId);
    }

    public Business createBusiness(Business business) {
        return businessRepository.save(business);
    }

    public Business updateBusiness(Business business) {
        return businessRepository.save(business);
    }

    public void deleteBusiness(Long id) {
        businessRepository.deleteById(id);
    }
}
