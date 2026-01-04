package com.ghazal.ims2.view.itemunitprice;

import com.ghazal.ims2.entity.ItemUnitPrice;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.validation.ValidationErrors;
import io.jmix.flowui.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "itemUnitPrices/:id", layout = MainView.class)
@ViewController("ItemUnitPrice.detail")
@ViewDescriptor("item-unit-price-detail-view.xml")
@EditedEntityContainer("itemUnitPriceDc")
public class ItemUnitPriceDetailView extends StandardDetailView<ItemUnitPrice> {

    private static final Logger log = LoggerFactory.getLogger(ItemUnitPriceDetailView.class);

    @Autowired
    private DataManager dataManager;

    @Autowired
    private Notifications notifications;

    @Subscribe
    public void onBeforeSave(BeforeSaveEvent event) {
        ItemUnitPrice entity = getEditedEntity();
        log.info("=== BEFORE SAVE ===");
        log.info("ID: {}", entity.getId());
        log.info("Item: {}", entity.getItem() != null ? entity.getItem().getName() : "null");
        log.info("Unit: {}", entity.getUnit() != null ? entity.getUnit().getName() : "null");
        log.info("Price: {}", entity.getPrice());
        log.info("Warehouse: {}", entity.getWarehouse() != null ? entity.getWarehouse() : "null");

        // Force save using DataManager to ensure changes are persisted
        try {
            ItemUnitPrice saved = dataManager.save(entity);
            log.info("Explicitly saved with DataManager - new price: {}", saved.getPrice());

            // Clear the data context to mark as no unsaved changes
            getViewData().getDataContext().clear();

            // Prevent default save since we already saved
            event.preventSave();

            // Manually close the view after successful save
            close(StandardOutcome.DISCARD);
        } catch (Exception e) {
            log.error("Error during explicit save", e);
            throw e;
        }
    }

    @Subscribe
    public void onAfterSave(AfterSaveEvent event) {
        log.info("=== AFTER SAVE - Success ===");
        notifications.create("Saved successfully!").show();
    }

    @Subscribe
    public void onValidation(ValidationEvent event) {
        ValidationErrors errors = event.getErrors();
        if (!errors.isEmpty()) {
            log.error("=== VALIDATION ERRORS ===");
            log.error("Errors: {}", errors);
        }
    }
}
