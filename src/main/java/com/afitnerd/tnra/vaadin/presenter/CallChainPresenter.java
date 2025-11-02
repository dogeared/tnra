package com.afitnerd.tnra.vaadin.presenter;

import com.afitnerd.tnra.model.GoToGuyPair;
import com.afitnerd.tnra.model.GoToGuySet;
import com.afitnerd.tnra.model.User;

import java.util.List;

public interface CallChainPresenter {
    String getFileUrl(String imgName);
    GoToGuySet getCurrentGoToGuySet();
    List<User> getAllActiveUsers();
    GoToGuySet createNewGoToGuySet(List<GoToGuyPair> pairs);
    boolean validatePair(User caller, User callee, List<GoToGuyPair> existingPairs);
    GoToGuySet addPairToSet(GoToGuySet set, GoToGuyPair pair);
    GoToGuySet removePairFromSet(GoToGuySet set, GoToGuyPair pair);
}
