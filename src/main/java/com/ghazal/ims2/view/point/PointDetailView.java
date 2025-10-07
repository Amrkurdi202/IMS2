package com.ghazal.ims2.view.point;

import com.ghazal.ims2.entity.Point;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;

@Route(value = "points/:id", layout = MainView.class)
@ViewController(id = "Point.detail")
@ViewDescriptor(path = "point-detail-view.xml")
@EditedEntityContainer("pointDc")
public class PointDetailView extends StandardDetailView<Point> {
}