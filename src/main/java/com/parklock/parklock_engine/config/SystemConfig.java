package com.parklock.parklock_engine.config;

import jakarta.persistence.*;

@Entity
@Table(name = "system_config")
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", unique = true, nullable = false)
    private String configKey; // e.g., "PRICE_PER_HOUR"

    @Column(name = "config_value", nullable = false)
    private String configValue; // e.g., "5.00"

    // Default constructor required by JPA
    public SystemConfig() {}

    // Constructor for easy initialization
    public SystemConfig(String configKey, String configValue) {
        this.configKey = configKey;
        this.configValue = configValue;
    }

    // Getters and Setters
    public Long getId() { 
        return id; 
    }
    
    public String getConfigKey() { 
        return configKey; 
    }
    
    public void setConfigKey(String configKey) { 
        this.configKey = configKey; 
    }
    
    public String getConfigValue() { 
        return configValue; 
    }
    
    public void setConfigValue(String configValue) { 
        this.configValue = configValue; 
    }
}