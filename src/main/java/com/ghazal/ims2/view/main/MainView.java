package com.ghazal.ims2.view.main;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.app.main.StandardMainView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

@Route("")
@ViewController("MainView")
@ViewDescriptor("main-view.xml")
public class MainView extends StandardMainView implements BeforeEnterObserver {

    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        // CurrentAuthentication -> Authentication -> getAuthorities()
        var auth = currentAuthentication.getAuthentication();

        boolean isLead = auth.getAuthorities().stream()
                .anyMatch(a ->
                        "ROLE_LEAD".equalsIgnoreCase(a.getAuthority()) ||
                                "lead".equalsIgnoreCase(a.getAuthority())
                );

        String path = event.getLocation().getPath();


        boolean isWorker = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_WORKER".equalsIgnoreCase(a.getAuthority())
                        || "worker".equalsIgnoreCase(a.getAuthority()));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority())
                        || "admin".equalsIgnoreCase(a.getAuthority()));


        if ((isAdmin || isLead) && (path == null || path.isBlank())) {
            event.forwardTo("lead-dashboard");
            return;
        }


        if (isWorker && (path == null || path.isBlank())) {
            event.forwardTo("attendance");
        }
    }
}
