package com.chrionline.chrionline.server.controllers;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.interfaces.IController;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.core.enums.StatutCommande;
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

    public String getAllOrders(AppRequest request) {
        try {
            logger.info("Admin Action: get all orders");
            java.util.List<com.chrionline.chrionline.shared.models.OrderSummary> orders = adminService.getAllOrders();
            return AppResponse.success(orders);
        } catch (Exception e) {
            logger.error("Error getting Admin Orders: ", e);
            return AppResponse.error("Error getting Admin Orders");
        }
    }

    public String updateOrderStatus(AppRequest request) {
        try {
            java.util.Map<String, Object> payloadMap = request.getPayloadAs(java.util.Map.class);
            if (payloadMap == null || !payloadMap.containsKey("idCommande") || !payloadMap.containsKey("statut")) {
                return AppResponse.badRequest("idCommande and statut are required");
            }
            Integer idCommande = ((Number) payloadMap.get("idCommande")).intValue();
            String statutStr = (String) payloadMap.get("statut");
            StatutCommande statut;
            try {
                statut = StatutCommande.valueOf(statutStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return AppResponse.badRequest("Invalid status provided");
            }

            logger.info("Admin Action: update order status for id={} to {}", idCommande, statut);
            boolean success = adminService.updateOrderStatus(idCommande, statut);
            if (success) {
                return AppResponse.success("Status updated successfully");
            } else {
                return AppResponse.error("Failed to update status, order may not exist");
            }
        } catch (Exception e) {
            logger.error("Error updating order status: ", e);
            return AppResponse.error("Error updating order status");
        }
    }
}
