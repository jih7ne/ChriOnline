package com.chrionline.chrionline.core.interfaces;

import com.chrionline.chrionline.core.config.ClientConfig;

public interface ConfigAware {
    void setClientConfig(ClientConfig clientConfig);
    void setViewManager(ViewManager viewManager);
}
