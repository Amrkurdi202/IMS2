package com.ghazal.ims2.entity;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Application-level key/value settings (n8n sync config, MCP token, sync revision).
 * Schema is managed by Liquibase changelog 11-integrations.xml.
 */
@JmixEntity(name = "AppSetting")
@Entity(name = "AppSetting")
@Table(name = "app_setting",
        uniqueConstraints = @UniqueConstraint(name = "ux_app_setting_key", columnNames = "SETTING_KEY"))
public class AppSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @InstanceName
    @Column(name = "SETTING_KEY", nullable = false, length = 64)
    private String key;

    @Column(name = "SETTING_VALUE", length = 4000)
    private String value;

    public Long getId() {
        return id;
    }

    public AppSetting setId(Long id) {
        this.id = id;
        return this;
    }

    public String getKey() {
        return key;
    }

    public AppSetting setKey(String key) {
        this.key = key;
        return this;
    }

    public String getValue() {
        return value;
    }

    public AppSetting setValue(String value) {
        this.value = value;
        return this;
    }
}
