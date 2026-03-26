package com.chrionline.server.controllers;

import com.chrionline.core.config.ServerConfig;
import com.chrionline.core.interfaces.IController;
import com.chrionline.network.protocol.AppRequest;
import com.chrionline.network.protocol.AppResponse;
import com.chrionline.core.enums.StatutCommande;
import com.chrionline.shared.models.DashboardStats;
import com.chrionline.server.services.AdminService;
import com.chrionline.shared.models.OrderSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class AdminController implements IController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final AdminService adminService;

    public AdminController() {
        this.adminService = ServerConfig.getService(AdminService.class);
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
            List<OrderSummary> orders = adminService.getAllOrders();
            return AppResponse.success(orders);
        } catch (Exception e) {
            logger.error("Error getting Admin Orders: ", e);
            return AppResponse.error("Error getting Admin Orders");
        }
    }

    public String updateOrderStatus(AppRequest request) {
        try {
            Map<String, Object> payloadMap = request.getPayloadAs(java.util.Map.class);
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
