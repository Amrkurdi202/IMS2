package com.ghazal.ims2.service.report;

import java.math.BigDecimal;

public class ProjectCostReportResult {

    private final BigDecimal totalPoints;
    private final BigDecimal pointsCost;
    private final BigDecimal clientAmount;

    public ProjectCostReportResult(BigDecimal totalPoints,
                                   BigDecimal pointsCost,
                                   BigDecimal clientAmount) {
        this.totalPoints = totalPoints;
        this.pointsCost = pointsCost;
        this.clientAmount = clientAmount;
    }

    public BigDecimal getTotalPoints() {
        return totalPoints;
    }

    public BigDecimal getPointsCost() {
        return pointsCost;
    }

    public BigDecimal getClientAmount() {
        return clientAmount;
    }
}
