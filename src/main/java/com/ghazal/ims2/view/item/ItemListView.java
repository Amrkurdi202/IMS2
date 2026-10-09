package com.ghazal.ims2.view.item;

import com.ghazal.ims2.entity.Item;
import com.ghazal.ims2.entity.ItemRepository;
import com.ghazal.ims2.entity.ItemUnitPrice;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import com.vaadin.flow.component.combobox.ComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Route(value = "items", layout = MainView.class)
@ViewController(id = "Item.list")
@ViewDescriptor(path = "item-list-view.xml")
@LookupComponent("itemsDataGrid")
@DialogMode(width = "64em")
public class ItemListView extends StandardListView<Item> {

    private static final SecureRandom RANDOM = new SecureRandom();

    @ViewComponent("itemsDataGrid")
    private DataGrid<Item> itemDataGrid;

    @ViewComponent("itemsDl")
    private CollectionLoader<Item> itemsDl;

    @Autowired
    private UiComponents uiComponents;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private Notifications notifications;

    @Autowired
    private MessageBundle messageBundle;

    @Subscribe
    public void onInit(InitEvent event) {
        itemDataGrid.addComponentColumn(item -> {
                    byte[] photo = item.getImage();
                    if (photo != null && photo.length > 0) {
                        JmixImage image = uiComponents.create(JmixImage.class);
                        image.setWidth("40px");
                        image.setHeight("40px");
                        StreamResource streamResource = new StreamResource(
                                "preview.jpg",
                                () -> new ByteArrayInputStream(photo)
                        );
                        image.setSrc(streamResource);
                        return image;
                    } else {
                        return new Span();
                    }
                })
                .setHeader("Photo")
                .setAutoWidth(true)
                .setFlexGrow(0);

        itemDataGrid.addComponentColumn(item -> {
                    Button printBtn = new Button(messageBundle.getMessage("item.printLabel"));
                    printBtn.setEnabled(item.getBarcode() != null && !item.getBarcode().isBlank());
                    printBtn.addClickListener(e -> requestPrint(item));
                    return printBtn;
                })
                .setHeader(messageBundle.getMessage("item.printLabel"))
                .setAutoWidth(true)
                .setFlexGrow(0);
    }

    /**
     * 0 or 1 ItemUnitPrice -> print the item barcode directly (no ambiguity).
     * Multiple IUPs -> ask which unit/warehouse barcode to print.
     */
    private void requestPrint(Item item) {
        List<ItemUnitPrice> iups = dataManager.load(ItemUnitPrice.class)
                .query("select p from ItemUnitPrice p join fetch p.unit join fetch p.warehouse "
                        + "where p.item = :i")
                .parameter("i", item)
                .list();

        if (iups.size() <= 1) {
            openLabel(item.getBarcode(), item.getName());
            return;
        }
        openIupPicker(item, iups);
    }

    private void openIupPicker(Item item, List<ItemUnitPrice> iups) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(messageBundle.getMessage("item.chooseIupPrint") + " — " + item.getName());
        dialog.setWidth("420px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        ComboBox<ItemUnitPrice> pick = new ComboBox<>();
        pick.setItems(iups);
        pick.setItemLabelGenerator(p -> p.getUnit().getName()
                + " @ " + p.getWarehouse().getName()
                + " (" + p.getPrice() + ")");
        pick.setWidth("100%");
        pick.setRequired(true);
        layout.add(pick);
        dialog.add(layout);

        Button cancel = new Button(messageBundle.getMessage("item.cancel"),
                e -> dialog.close());
        Button print = new Button(messageBundle.getMessage("item.printLabel"), e -> {
            ItemUnitPrice chosen = pick.getValue();
            if (chosen == null) {
                pick.setInvalid(true);
                return;
            }
            ensureBarcode(chosen);
            String label = item.getName() + " (" + chosen.getUnit().getName() + ")";
            openLabel(chosen.getBarcode(), label);
            dialog.close();
        });
        print.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(cancel, print);
        dialog.open();
    }

    private void ensureBarcode(ItemUnitPrice iup) {
        String barcode = iup.getBarcode() == null ? "" : iup.getBarcode().trim();
        if (!barcode.isEmpty()) {
            return;
        }
        Set<String> used = new HashSet<>();
        dataManager.load(ItemUnitPrice.class).query("select p from ItemUnitPrice p").list().forEach(p -> {
            if (p.getBarcode() != null && !p.getBarcode().isBlank()) used.add(p.getBarcode());
        });
        dataManager.load(Item.class).query("select i from Item i").list().forEach(i -> {
            if (i.getBarcode() != null && !i.getBarcode().isBlank()) used.add(i.getBarcode());
        });
        String code = nextCode(used);
        iup.setBarcode(code);
        dataManager.save(iup);
    }

    @Subscribe("generateBarcodesBtn")
    public void onGenerateBarcodesBtnClick(com.vaadin.flow.component.ClickEvent<Button> event) {
        List<Item> items = new ArrayList<>();
        itemRepository.findAll().forEach(items::add);
        Set<String> used = new HashSet<>();
        for (Item item : items) {
            if (item.getBarcode() != null && !item.getBarcode().isBlank()) {
                used.add(item.getBarcode());
            }
        }
        int generated = 0;
        for (Item item : items) {
            if (item.getBarcode() != null && !item.getBarcode().isBlank()) {
                continue;
            }
            String code = nextCode(used);
            item.setBarcode(code);
            used.add(code);
            itemRepository.save(item);
            generated++;
        }
        notifications.create(messageBundle.getMessage("item.generatedCount")
                        .replace("{0}", String.valueOf(generated)))
                .withType(Notifications.Type.SUCCESS)
                .show();
        itemsDl.load();
    }

    @Subscribe("openLabelsBtn")
    public void onOpenLabelsBtnClick(com.vaadin.flow.component.ClickEvent<Button> event) {
        getUI().ifPresent(ui -> ui.getPage().open("/labels/index.html", "_blank"));
    }

    private void openLabel(String barcode, String name) {
        String b = URLEncoder.encode(barcode == null ? "" : barcode, StandardCharsets.UTF_8);
        String n = URLEncoder.encode(name == null ? "" : name, StandardCharsets.UTF_8);
        getUI().ifPresent(ui -> ui.getPage()
                .open("/labels/index.html?barcode=" + b + "&product=" + n, "_blank"));
    }

    private static String nextCode(Set<String> used) {
        for (int attempt = 0; attempt < 100; attempt++) {
            StringBuilder sb = new StringBuilder("2");
            for (int i = 0; i < 12; i++) {
                sb.append(RANDOM.nextInt(10));
            }
            String candidate = sb.toString();
            if (!used.contains(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("cannot generate unique barcode");
    }
}