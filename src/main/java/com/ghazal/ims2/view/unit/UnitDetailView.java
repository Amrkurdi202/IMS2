package com.ghazal.ims2.view.unit;

import com.ghazal.ims2.entity.Unit;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;

@Route(value = "units/:id", layout = MainView.class)
@ViewController(id = "Unit.detail")
@ViewDescriptor(path = "unit-detail-view.xml")
@EditedEntityContainer("unitDc")
public class UnitDetailView extends StandardDetailView<Unit> {
}