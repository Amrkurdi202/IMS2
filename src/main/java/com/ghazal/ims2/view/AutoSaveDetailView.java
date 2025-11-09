package com.ghazal.ims2.view;

import com.vaadin.flow.component.DetachEvent;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewComponent;

public abstract class AutoSaveDetailView<T> extends StandardDetailView<T> {

    @ViewComponent
    private DataContext dataContext;

    @Override
    protected void onDetach(DetachEvent event) {
        if (dataContext != null) {
            dataContext.save(true);
        }
        super.onDetach(event);
    }
}
