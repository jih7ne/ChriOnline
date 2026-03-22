package com.chrionline.chrionline.server.controllers;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.interfaces.IController;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.server.data.models.Adresse;
import com.chrionline.chrionline.server.data.models.DashboardStats;
import com.chrionline.chrionline.server.services.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AdminController implements IController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final AdminService adminService;

    public AdminController() {
        this.adminService = AppConfig.getService(AdminService.class);
    }

    public String getStats(AppRequest request) {
        try {
            logger.info("Getting Admin Dashboard Stats");
            DashboardStats dashboardStats = adminService.getDashboardStats();
            return AppResponse.success(dashboardStats);
        } catch (Exception e) {
            logger.error("Error getting Admin Dashboard Stats: ", e);
            return AppResponse.error("Error getting Admin Dashboard Stats: ", e);
        }
    }
}
