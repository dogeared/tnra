package com.afitnerd.tnra.vaadin.presenter;

import com.afitnerd.tnra.model.GoToGuySet;
import com.afitnerd.tnra.repository.GoToGuySetRepository;
import com.afitnerd.tnra.service.FileStorageService;
import org.springframework.stereotype.Service;

@Service
public class CallChainPresenterImpl implements CallChainPresenter {

    private final GoToGuySetRepository goToGuySetRepository;
    private final FileStorageService fileStorageService;

    public CallChainPresenterImpl(GoToGuySetRepository goToGuySetRepository, FileStorageService fileStorageService) {
        this.goToGuySetRepository = goToGuySetRepository;
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
}
