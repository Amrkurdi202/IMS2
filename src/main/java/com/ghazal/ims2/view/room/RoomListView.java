package com.ghazal.ims2.view.room;

import com.ghazal.ims2.entity.Room;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;

@Route(value = "rooms", layout = MainView.class)
@ViewController(id = "Room.list")
@ViewDescriptor(path = "room-list-view.xml")
@LookupComponent("roomsDataGrid")
@DialogMode(width = "64em")
public class RoomListView extends StandardListView<Room> {
}