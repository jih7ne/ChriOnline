package com.chrionline.chrionline.core.config;

import com.chrionline.chrionline.core.interfaces.IController;
import com.chrionline.chrionline.server.controllers.AuthController;
import com.chrionline.chrionline.server.repositories.ProduitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class AppConfig {

    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    private static Connection connection;

    private static final Map<Class<?>, Object> repositories = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Object> services = new ConcurrentHashMap<>();
    private static final Map<String, IController> controllers = new ConcurrentHashMap<>();


    static {

        try {

            Properties props = new Properties();

            InputStream input = AppConfig.class
                    .getClassLoader()
                    .getResourceAsStream("application.properties");

            props.load(input);

            String url = props.getProperty("db.url");
            String username = props.getProperty("db.username");
            String password = props.getProperty("db.password");
            String driver = props.getProperty("db.driver");

            Class.forName(driver);

            connection = DriverManager.getConnection(url, username, password);

            logger.info("Database connection established");



        } catch (Exception e) {

            logger.error("Failed to initialize database connection", e);

        }


        logger.info("Repositories initialized");

    }


    public static <T> T getRepo(Class<T> repoClass) {
        return repoClass.cast(repositories.get(repoClass));
    }

    public static <T> void registerRepo(Class<T> clazz, T instance) {
        repositories.put(clazz, instance);
    }

    public static <T> void removeRepo(Class<T> clazz) {
        repositories.remove(clazz);
    }


    public static <T> T getService(Class<T> serviceClass) {
        return serviceClass.cast(services.get(serviceClass));
    }



    public static <T> void registerService(Class<T> clazz, T instance) {
        services.put(clazz, instance);
    }

    public static <T> void removeService(Class<T> clazz) {
        services.remove(clazz);
    }


    public static <T> void registerController(String controllerName, IController instance) {
        controllers.put(controllerName, instance);
    }

    public static IController getController(String name) {
        return controllers.get(name);
    }


    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database connection is not available");
        }
        return connection;
    }

    public static Logger getLogger() {
        return logger;
    }

}