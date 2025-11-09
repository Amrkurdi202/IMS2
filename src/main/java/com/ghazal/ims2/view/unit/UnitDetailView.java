package com.ghazal.ims2.view.unit;

import com.ghazal.ims2.entity.Unit;
import com.ghazal.ims2.entity.UnitRepository;
import com.ghazal.ims2.view.AutoSaveDetailView;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "units/:id", layout = MainView.class)
@ViewController(id = "Unit.detail")
@ViewDescriptor(path = "unit-detail-view.xml")
@EditedEntityContainer("unitDc")
public class UnitDetailView extends AutoSaveDetailView<Unit> {
    
    @Autowired
    private UnitRepository unitRepository;

    @Subscribe(id = "saveAndCloseButton", subject = "clickListener")
    public void onSaveAndCloseButtonClick(final ClickEvent<JmixButton> event) {
        unitRepository.save(getEditedEntity());
    }
}
