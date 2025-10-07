package com.ghazal.ims2.view.point;

import com.ghazal.ims2.entity.Point;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;

@Route(value = "points", layout = MainView.class)
@ViewController(id = "Point.list")
@ViewDescriptor(path = "point-list-view.xml")
@LookupComponent("pointsDataGrid")
@DialogMode(width = "64em")
public class PointListView extends StandardListView<Point> {
}