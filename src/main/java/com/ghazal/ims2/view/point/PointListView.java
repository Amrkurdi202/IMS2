package com.ghazal.ims2.view.point;

import com.ghazal.ims2.entity.Point;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;

@Route(value = "points", layout = MainView.class)
@ViewController(id = "Point.list")
@ViewDescriptor(path = "point-list-view.xml")
@LookupComponent("pointsDataGrid")
public class PointListView extends StandardListView<Point> {

    @ViewComponent("pointsDataGrid")
    private DataGrid<Point> pointsDataGrid;

    @Autowired
    private UiComponents uiComponents;

    @Subscribe
    public void onInit(InitEvent event) {
        // use a keyed column from XML if you want; but this works too:
        pointsDataGrid.addComponentColumn(point -> {
                    byte[] photo = point != null ? point.getImage() : null;
                    if (photo != null && photo.length > 0) {
                        JmixImage<?> image = uiComponents.create(JmixImage.class);
                        image.setWidth("40px");
                        image.setHeight("40px");
                        image.setSrc(new StreamResource(
                                "preview.jpg",
                                () -> new ByteArrayInputStream(photo)
                        ));
                        return image;
                    }
                    return new Span();
                })
                .setHeader("Photo")
                .setAutoWidth(true)
                .setFlexGrow(0);
    }
}
