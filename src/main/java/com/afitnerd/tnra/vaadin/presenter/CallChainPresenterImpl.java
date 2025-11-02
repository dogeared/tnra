package com.afitnerd.tnra.vaadin.presenter;

import com.afitnerd.tnra.model.GoToGuyPair;
import com.afitnerd.tnra.model.GoToGuySet;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.GoToGuySetRepository;
import com.afitnerd.tnra.repository.UserRepository;
import com.afitnerd.tnra.service.FileStorageService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.StreamSupport;

@Service
public class CallChainPresenterImpl implements CallChainPresenter {

    private final GoToGuySetRepository goToGuySetRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public CallChainPresenterImpl(GoToGuySetRepository goToGuySetRepository, 
                                  UserRepository userRepository,
                                  FileStorageService fileStorageService) {
        this.goToGuySetRepository = goToGuySetRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public String getFileUrl(String imgName) {
        return fileStorageService.getFileUrl(imgName);
    }

    @Override
    public GoToGuySet getCurrentGoToGuySet() {
        return goToGuySetRepository.findTopByOrderByStartDateDesc();
    }
    
    @Override
    public List<User> getAllActiveUsers() {
        return StreamSupport.stream(userRepository.findByActiveTrue().spliterator(), false)
                .collect(ArrayList::new, (list, user) -> list.add(user), ArrayList::addAll);
    }
    
    @Override
    public GoToGuySet createNewGoToGuySet(List<GoToGuyPair> pairs) {
        GoToGuySet newSet = new GoToGuySet();
        newSet.setStartDate(new Date());
        
        // Save the set first
        newSet = goToGuySetRepository.save(newSet);
        
        // Associate pairs with the set
        final GoToGuySet savedSet = newSet;
        pairs.forEach(pair -> pair.setGoToGuySet(savedSet));
        newSet.setGoToGuyPairs(pairs);
        
        // Save again with pairs
        return goToGuySetRepository.save(newSet);
    }
    
    @Override
    public boolean validatePair(User caller, User callee, List<GoToGuyPair> existingPairs) {
        if (caller == null || callee == null) {
            return false;
        }

        // Rule 1: A person can't call themselves
        if (caller.getId().equals(callee.getId())) {
            return false;
        }

        // Rule 2: A person can't call someone that is already being called by someone else
        boolean calleeAlreadyAssigned = existingPairs.stream()
            .anyMatch(pair -> pair.getCallee().getId().equals(callee.getId()));

        if (calleeAlreadyAssigned) {
            return false;
        }

        return true;
    }

    @Override
    public GoToGuySet addPairToSet(GoToGuySet set, GoToGuyPair pair) {
        pair.setGoToGuySet(set);

        if (set.getGoToGuyPairs() == null) {
            set.setGoToGuyPairs(new ArrayList<>());
        }
        set.getGoToGuyPairs().add(pair);

        return goToGuySetRepository.save(set);
    }

    @Override
    public GoToGuySet removePairFromSet(GoToGuySet set, GoToGuyPair pair) {
        if (set.getGoToGuyPairs() != null) {
            set.getGoToGuyPairs().remove(pair);
        }
        return goToGuySetRepository.save(set);
    }
}
