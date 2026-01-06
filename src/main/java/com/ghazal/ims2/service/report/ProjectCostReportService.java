package com.ghazal.ims2.service.report;

import com.ghazal.ims2.entity.Item;
import com.ghazal.ims2.entity.ItemUnitPrice;
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

        StringBuilder jpql = new StringBuilder(
                "select oi from ORDER_ITEM oi " +
                        "left join fetch oi.item " +
                        "left join fetch oi.unit " +
                        "where oi.order.project = :project " +
                        "and oi.order.createdDate >= :fromDate " +
                        "and oi.order.createdDate <= :toDate"
        );

        if (warehouse != null) {
            jpql.append(" and oi.order.warehouse = :warehouse");
        }

        var loader = dataManager.load(OrderItem.class)
                .query(jpql.toString())
                .parameter("project", project)
                .parameter("fromDate", fromDate)
                .parameter("toDate", toDate);

        if (warehouse != null) {
            loader.parameter("warehouse", warehouse);
        }

        List<OrderItem> orderItems = loader.list();

        BigDecimal clientAmount = BigDecimal.ZERO;

        for (OrderItem orderItem : orderItems) {
            BigDecimal quantity = orderItem.getQuantity();
            if (quantity == null) {
                continue;
            }

            BigDecimal unitPrice = orderItem.getUnitPriceAtOrder();

            if (unitPrice == null) {
                Item item = orderItem.getItem();
                if (item != null && warehouse != null) {
                    unitPrice = findItemUnitPrice(item, warehouse);
                }
            }

            if (unitPrice == null) {
                continue;
            }

            clientAmount = clientAmount.add(quantity.multiply(unitPrice));
        }

        return new ProjectCostReportResult(
                totalPoints,
                pointsCost,
                clientAmount,
                orderItems
        );
    }

    @Nullable
    private BigDecimal findItemUnitPrice(Item item, @Nullable Warehouse warehouse) {
        if (warehouse == null) {
            return null;
        }

        List<ItemUnitPrice> prices = dataManager.load(ItemUnitPrice.class)
                .query("select p from ItemUnitPrice p where p.item = :item and p.warehouse = :warehouse")
                .parameter("item", item)
                .parameter("warehouse", warehouse)
                .list();

        if (prices.isEmpty()) {
            return null;
        }

        return prices.get(0).getPrice();
    }
}
