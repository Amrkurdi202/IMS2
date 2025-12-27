package com.ghazal.ims2.view.unit;

import com.ghazal.ims2.entity.Unit;
import com.ghazal.ims2.entity.UnitRepository;
import com.ghazal.ims2.view.AutoSaveDetailView;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;

import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "units/:id", layout = MainView.class)
@ViewController("Unit.detail")
@ViewDescriptor("unit-detail-view.xml")
@EditedEntityContainer("unitDc")
public class UnitDetailView extends AutoSaveDetailView<Unit> {

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private Notifications notifications;

    @Subscribe(id = "saveAndCloseButton", subject = "clickListener")
    public void onSaveAndCloseButtonClick(ClickEvent<JmixButton> event) {
        try {
            unitRepository.save(getEditedEntity());
            closeWithSave();
        } catch (Exception e) {
            notifications.create("Save failed: " + e.getMessage())
                    .withType(Notifications.Type.ERROR)
                    .show();
        }
    }
}
