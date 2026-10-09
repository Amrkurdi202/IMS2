package com.ghazal.ims2.security;

import io.jmix.core.JmixSecurityFilterChainOrder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * This configuration complements standard security configurations that come from Jmix modules (security-flowui, oidc,
 * authserver).
 * <p>
 * You can configure custom API endpoints security by defining {@link SecurityFilterChain} beans in this class.
 * In most cases, custom SecurityFilterChain must be applied first, so the proper
 * {@link org.springframework.core.annotation.Order} should be defined for the bean. The order value from the
 * {@link io.jmix.core.JmixSecurityFilterChainOrder#CUSTOM} is guaranteed to be smaller than any other filter chain
 * order from Jmix.
 * <p>
 * Example:
 *
 * <pre>
 * &#064;Bean
 * &#064;Order(JmixSecurityFilterChainOrder.CUSTOM)
 * SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
 *     http.securityMatcher("/public/**")
 *             .authorizeHttpRequests(authorize ->
 *                     authorize.anyRequest().permitAll()
 *             );
 *     return http.build();
 * }
 * </pre>
 *
 * @see io.jmix.securityflowui.security.FlowuiVaadinWebSecurity
 */
@Configuration
public class IMS2SecurityConfiguration {

    /**
     * Makes static images available on public pages, e.g. the landing page and the privacy policy.
     */
    @Bean
    @Order(JmixSecurityFilterChainOrder.CUSTOM)
    SecurityFilterChain publicImagesFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/images/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }

    /**
     * Exposes the MCP endpoint to external AI clients. The endpoint itself authenticates
     * each request with a Bearer token stored in app_setting (mcp.token), so no session
     * login is required here. CSRF is irrelevant for bearer-token JSON-RPC calls.
     */
    @Bean
    @Order(JmixSecurityFilterChainOrder.CUSTOM + 1)
    SecurityFilterChain mcpFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/mcp", "/mcp/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }
}
