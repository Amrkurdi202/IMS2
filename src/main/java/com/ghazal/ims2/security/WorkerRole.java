package com.ghazal.ims2.security;

import com.ghazal.ims2.entity.AttendanceDay;
import com.ghazal.ims2.entity.AttendanceRecord;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.model.SecurityScope;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

@ResourceRole(
        name = "Worker",
        code = WorkerRole.CODE,
        scope = SecurityScope.UI   // role is for UI
)
public interface WorkerRole {

    String CODE = "worker";

    // ---- UI access (menu + view) ----
    // "application" is the root group in menu.xml
    // "AttendanceView" is your worker screen
    @MenuPolicy(menuIds = {"application", "AttendanceView"})
    @ViewPolicy(viewIds = {"AttendanceView"})
    void attendanceScreens();

    // ---- Entities that worker can use ----
    @EntityPolicy(entityClass = AttendanceDay.class,
            actions = {EntityPolicyAction.READ, EntityPolicyAction.CREATE})
    @EntityAttributePolicy(entityClass = AttendanceDay.class,
            attributes = "*",
            action = EntityAttributePolicyAction.MODIFY)
    void attendanceDay();

    @EntityPolicy(entityClass = AttendanceRecord.class,
            actions = {EntityPolicyAction.READ, EntityPolicyAction.CREATE})
    @EntityAttributePolicy(entityClass = AttendanceRecord.class,
            attributes = "*",
            action = EntityAttributePolicyAction.MODIFY)
    void attendanceRecord();
}
