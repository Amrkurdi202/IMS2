package com.ghazal.ims2.service.report;

import com.ghazal.ims2.entity.OrderItem;

import java.math.BigDecimal;
import java.util.List;

public record ProjectCostReportResult(BigDecimal totalPoints, BigDecimal pointsCost, BigDecimal clientAmount,
                                      List<OrderItem> orderItems) {

}
