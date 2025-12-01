package com.ghazal.ims2.service.report;

import com.ghazal.ims2.entity.Item;
import com.ghazal.ims2.entity.ItemUnitPrice;
import com.ghazal.ims2.entity.Order;
import com.ghazal.ims2.entity.OrderItem;
import com.ghazal.ims2.entity.Project;
import com.ghazal.ims2.entity.ProjectPoint;
import com.ghazal.ims2.entity.Room;
import com.ghazal.ims2.entity.Warehouse;
import io.jmix.core.DataManager;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class ProjectCostReportService {

    private final DataManager dataManager;

    public ProjectCostReportService(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public ProjectCostReportResult calculateProjectCost(
            Project project,
            OffsetDateTime fromDate,
            OffsetDateTime toDate,
            @Nullable Warehouse warehouse,
            BigDecimal pointValue
    ) {

        // -------------------------------------------------
        // A) Total points for the project
        //    = sum of ProjectPoint.quantity for all rooms
        // -------------------------------------------------
        BigDecimal totalPoints = BigDecimal.ZERO;

        if (project.getRooms() != null) {
            for (Room room : project.getRooms()) {
                List<ProjectPoint> roomPoints = room.getPoints();
                if (roomPoints == null) {
                    continue;
                }

                for (ProjectPoint projectPoint : roomPoints) {
                    BigDecimal qty = projectPoint.getQuantity();
                    if (qty != null) {
                        totalPoints = totalPoints.add(qty);
                    }
                }
            }
        }

        BigDecimal pointsCost = totalPoints.multiply(pointValue);

        // -------------------------------------------------
        // B) Amount client should pay from orders
        // -------------------------------------------------

        // NOTE: Order entity is @Entity(name = "ims_Order"), so we use ims_Order here.
        // TODO: replace "o.date" with your actual order date field if different.
        StringBuilder jpql = new StringBuilder(
                "select o from ims_Order o " +
                        "where o.project = :project " +
                        "and o.createdDate >= :fromDate " +
                        "and o.createdDate <= :toDate"
        );

        if (warehouse != null) {
            jpql.append(" and o.warehouse = :warehouse");
        }

        var loader = dataManager.load(Order.class)
                .query(jpql.toString())
                .parameter("project", project)
                .parameter("fromDate", fromDate)
                .parameter("toDate", toDate);

        if (warehouse != null) {
            loader.parameter("warehouse", warehouse);
        }

        List<Order> orders = loader.list();

        BigDecimal clientAmount = BigDecimal.ZERO;

        for (Order order : orders) {
            if (order.getItems() == null) {
                continue;
            }

            for (OrderItem orderItem : order.getItems()) {
                BigDecimal quantity = orderItem.getQuantity();
                Item item = orderItem.getItem();

                if (quantity == null || item == null) {
                    continue;
                }

                // 1) Prefer the frozen price stored on the order item
                BigDecimal unitPrice = orderItem.getUnitPriceAtOrder();

                // 2) Fallback for older data where unitPriceAtOrder is null
                if (unitPrice == null) {
                    unitPrice = findItemUnitPrice(item, order.getWarehouse());
                }

                if (unitPrice == null) {
                    // still no price → skip this line
                    continue;
                }

                clientAmount = clientAmount.add(quantity.multiply(unitPrice));
            }
        }

        return new ProjectCostReportResult(
                totalPoints,
                pointsCost,
                clientAmount
        );
    }

    /**
     * Finds the current price for the given item in the given warehouse
     * using ItemUnitPrice. Used as a fallback when unitPriceAtOrder is null.
     */
    @Nullable
    private BigDecimal findItemUnitPrice(Item item, @Nullable Warehouse warehouse) {
        if (warehouse == null) {
            return null;
        }

        List<ItemUnitPrice> prices = dataManager.load(ItemUnitPrice.class)
                .query("select p from ItemUnitPrice p " +
                        "where p.item = :item and p.warehouse = :warehouse")
                .parameter("item", item)
                .parameter("warehouse", warehouse)
                .list();

        if (prices.isEmpty()) {
            return null;
        }

        return prices.get(0).getPrice();
    }
}
