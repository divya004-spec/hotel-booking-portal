package com.example.hotelbooking.config;

import com.example.hotelbooking.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InventoryInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InventoryInitializer.class);

    private final InventoryService inventoryService;

    public InventoryInitializer(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    public void run(ApplicationArguments args) {
        topUp();
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void topUp() {
        int created = inventoryService.ensureInventoryForAllHotels();
        if (created > 0) {
            log.info("Created {} room inventory rows", created);
        }
    }
}
