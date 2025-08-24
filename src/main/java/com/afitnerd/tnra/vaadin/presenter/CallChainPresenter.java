package com.afitnerd.tnra.vaadin.presenter;

import com.afitnerd.tnra.model.GoToGuySet;

public interface CallChainPresenter {
    String getFileUrl(String imgName);
    GoToGuySet getCurrentGoToGuySet();
}
