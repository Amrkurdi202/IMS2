package com.ghazal.ims2.view.room;

import com.ghazal.ims2.entity.Room;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;

@Route(value = "rooms/:id", layout = MainView.class)
@ViewController(id = "Room.detail")
@ViewDescriptor(path = "room-detail-view.xml")
@EditedEntityContainer("roomDc")
public class RoomDetailView extends StandardDetailView<Room> {
}