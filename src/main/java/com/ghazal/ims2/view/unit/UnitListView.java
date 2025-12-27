package com.ghazal.ims2.view.unit;

import com.ghazal.ims2.entity.Unit;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;

@Route(value = "units", layout = MainView.class)
@ViewController(id = "Unit.list")
@ViewDescriptor(path = "unit-list-view.xml")
@LookupComponent("unitsDataGrid")
public class UnitListView extends StandardListView<Unit> {

    @ViewComponent
    private CollectionLoader<Unit> unitsDl;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        unitsDl.load(); //
    }
}
