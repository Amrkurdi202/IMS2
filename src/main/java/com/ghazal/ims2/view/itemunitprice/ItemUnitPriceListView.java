package com.ghazal.ims2.view.itemunitprice;

import com.ghazal.ims2.entity.Item;
import com.ghazal.ims2.entity.ItemUnitPrice;
import com.ghazal.ims2.entity.Warehouse;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Route(value = "itemUnitPrices", layout = MainView.class)
@ViewController(id = "ItemUnitPrice.list")
@ViewDescriptor(path = "item-unit-price-list-view.xml")
@LookupComponent("itemUnitPricesDataGrid")
public class ItemUnitPriceListView extends StandardListView<ItemUnitPrice> {

    private static final SecureRandom RANDOM = new SecureRandom();

    @ViewComponent
    private CollectionLoader<ItemUnitPrice> itemUnitPricesDl;

    @ViewComponent("itemUnitPricesDataGrid")
    private io.jmix.flowui.component.grid.DataGrid<ItemUnitPrice> dataGrid;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private Notifications notifications;

    @Autowired
    private MessageBundle messageBundle;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        // IMPORTANT: set the parameter even if null, to avoid "argument not found"
        itemUnitPricesDl.setParameter("warehouse", null);
        itemUnitPricesDl.load();
    }

    @Subscribe
    public void onInit(InitEvent event) {
        dataGrid.addComponentColumn(iup -> {
                    Button printBtn = new Button(messageBundle.getMessage("itemUnitPrice.printLabel"));
                    printBtn.setEnabled(iup.getBarcode() != null && !iup.getBarcode().isBlank());
                    printBtn.addClickListener(e -> openLabel(iup));
                    return printBtn;
                })
                .setHeader(messageBundle.getMessage("itemUnitPrice.printLabel"))
                .setAutoWidth(true)
                .setFlexGrow(0);
    }

    @Subscribe("generateBarcodesBtn")
    public void onGenerateBarcodesBtnClick(com.vaadin.flow.component.ClickEvent<Button> event) {
        // Load ALL rows from the DB — the collection container only holds the current page.
        List<ItemUnitPrice> rows = new ArrayList<>();
        dataManager.load(ItemUnitPrice.class)
                .query("select p from ItemUnitPrice p order by p.id")
                .list()
                .forEach(rows::add);

        Set<String> used = new HashSet<>();
        dataManager.load(ItemUnitPrice.class).query("select p from ItemUnitPrice p").list().forEach(p -> {
            if (p.getBarcode() != null && !p.getBarcode().isBlank()) used.add(p.getBarcode());
        });
        dataManager.load(Item.class).query("select i from Item i").list().forEach(i -> {
            if (i.getBarcode() != null && !i.getBarcode().isBlank()) used.add(i.getBarcode());
        });

        int generated = 0;
        for (ItemUnitPrice p : rows) {
            if (p.getBarcode() != null && !p.getBarcode().isBlank()) continue;
            String code = nextCode(used);
            p.setBarcode(code);
            used.add(code);
            dataManager.save(p);
            generated++;
        }
        notifications.create(messageBundle.getMessage("itemUnitPrice.generatedCount")
                        .replace("{0}", String.valueOf(generated)))
                .withType(Notifications.Type.SUCCESS)
                .show();
        itemUnitPricesDl.load();
    }

    private void openLabel(ItemUnitPrice iup) {
        String itemUnitLabel = iup.getItem().getName()
                + (iup.getUnit() != null ? " (" + iup.getUnit().getName() + ")" : "");
        String barcode = URLEncoder.encode(iup.getBarcode(), StandardCharsets.UTF_8);
        String name = URLEncoder.encode(itemUnitLabel, StandardCharsets.UTF_8);
        getUI().ifPresent(ui -> ui.getPage()
                .open("/labels/index.html?barcode=" + barcode + "&product=" + name, "_blank"));
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

    public void setWarehouse(Warehouse warehouse) {
        itemUnitPricesDl.setParameter("warehouse", warehouse);
        itemUnitPricesDl.load();
    }
}