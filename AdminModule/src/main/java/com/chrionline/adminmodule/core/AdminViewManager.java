package com.chrionline.adminmodule.core;

import com.chrionline.network.tcp.TCPClient;

import java.util.Map;
import java.util.function.Consumer;

public interface AdminViewManager {
    void showAdminLoginView();
    void showAdminView(Map<String, Object> userData);
    void showAdminDashboard();

    void showLoginView();
}
