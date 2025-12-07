package com.ghazal.ims2.view.unit;

import com.ghazal.ims2.entity.Unit;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "units/:id", layout = MainView.class)
@ViewController("Unit.detail")
@ViewDescriptor("unit-detail-view.xml")
@EditedEntityContainer("unitDc")
public class UnitDetailView extends StandardDetailView<Unit> {
    // No custom save logic needed:
    // the <button id="saveAndCloseButton" action="saveAction"/> in XML
    // will handle validation + saving + closing automatically.
}
