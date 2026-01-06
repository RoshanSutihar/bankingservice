package com.roshansutihar.bankingservice.service;

import com.roshansutihar.bankingservice.entity.Individual;
import com.roshansutihar.bankingservice.repository.IndividualRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IndividualService {

    @Autowired
    private IndividualRepository individualRepository;

    public List<Individual> getAllIndividuals() {
        return individualRepository.findAll();
    }

    public Optional<Individual> getIndividualById(Long id) {
        return individualRepository.findById(id);
    }

    public Optional<Individual> getIndividualByUserId(Long userId) {
        return individualRepository.findByUserId(userId);
    }

    public Optional<Individual> getIndividualBySsn(String ssn) {
        return individualRepository.findBySsn(ssn);
    }

    public Individual createIndividual(Individual individual) {
        return individualRepository.save(individual);
    }

    public Individual updateIndividual(Individual individual) {
        return individualRepository.save(individual);
    }

    public void deleteIndividual(Long id) {
        individualRepository.deleteById(id);
    }
}
