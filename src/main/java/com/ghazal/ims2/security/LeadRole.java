package com.ghazal.ims2.security;

import com.ghazal.ims2.entity.Item;
import com.ghazal.ims2.entity.Order;
import com.ghazal.ims2.entity.OrderItem;
import com.ghazal.ims2.entity.Project;
import com.ghazal.ims2.entity.Warehouse;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.model.SecurityScope;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

@ResourceRole(
        name = "Lead",
        code = LeadRole.CODE,
        scope = SecurityScope.UI
)
public interface LeadRole {

    String CODE = "lead";

    // ---- UI access (menus + views) ----

    @MenuPolicy(menuIds = {
            "application",   // root menu group from menu-config.xml
            "OrdersMenu"     // Orders menu item id from menu-config.xml
    })
    @ViewPolicy(viewIds = {
            // Orders screens
            "ims_order.list",
            "ims_Order.detail",

            // Order item detail (only if you still use it)
            "OrderItem.detail",

            // Lookups
            "Warehouse.list",
            "Project.list",
            "Item.list"
    })
    void uiAccess();

    // ---- Entity permissions ----

    @EntityPolicy(entityClass = Order.class,
            actions = {EntityPolicyAction.READ, EntityPolicyAction.CREATE, EntityPolicyAction.UPDATE})
    @EntityAttributePolicy(entityClass = Order.class,
            attributes = "*",
            action = EntityAttributePolicyAction.MODIFY)
    void order();

    @EntityPolicy(entityClass = OrderItem.class,
            actions = {
                    EntityPolicyAction.READ,
                    EntityPolicyAction.CREATE,
                    EntityPolicyAction.UPDATE,
                    EntityPolicyAction.DELETE   // ✅ now valid
            })
    @EntityAttributePolicy(entityClass = OrderItem.class,
            attributes = "*",
            action = EntityAttributePolicyAction.MODIFY)
    void orderItem();

    @EntityPolicy(entityClass = Item.class,
            actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = Item.class,
            attributes = "*",
            action = EntityAttributePolicyAction.VIEW)
    void item();

    @EntityPolicy(entityClass = Warehouse.class,
            actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = Warehouse.class,
            attributes = "*",
            action = EntityAttributePolicyAction.VIEW)
    void warehouse();

    @EntityPolicy(entityClass = Project.class,
            actions = {EntityPolicyAction.READ})
    @EntityAttributePolicy(entityClass = Project.class,
            attributes = "*",
            action = EntityAttributePolicyAction.VIEW)
    void project();
}
