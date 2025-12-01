package com.ghazal.ims2.security;

import com.ghazal.ims2.entity.Order;
import io.jmix.security.role.annotation.RowLevelRole;
import io.jmix.security.role.annotation.JpqlRowLevelPolicy;

@RowLevelRole(
        name = "Lead: only own orders",
        code = LeadOrdersRowLevelRole.CODE
)
public interface LeadOrdersRowLevelRole {

    String CODE = "lead-own-orders";

    @JpqlRowLevelPolicy(
            entityClass = Order.class,
            where = "{E}.user.id = :current_user_id"
    )
    void ordersCreatedByCurrentUser();
}
