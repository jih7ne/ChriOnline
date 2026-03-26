package com.chrionline.server.data;

import com.chrionline.core.interfaces.IBaseRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public abstract class JdbcRepository<T> implements IBaseRepository<T> {

    protected Connection connection;
    protected RowMapper<T> rowMapper;
    protected String tableName;

    public JdbcRepository(Connection connection, String tableName, RowMapper<T> rowMapper) {
        this.connection = connection;
        this.tableName = tableName;
        this.rowMapper = rowMapper;
    }

    public int count() {
        String query = "SELECT COUNT(*) FROM " + tableName;

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            if (resultSet.next()) {
                return resultSet.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public T getById(String id) {
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, id);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rowMapper.mapRow(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public List<T> getAll() {
        String sql = "SELECT * FROM " + tableName;

        List<T> results = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                results.add(rowMapper.mapRow(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return results;
    }

    @Override
    public void delete(String id) {
        String sql = "DELETE FROM " + tableName + " WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void clear() {
        String sql = "DELETE FROM " + tableName;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Override
    public abstract void add(T item);

    @Override
    public abstract void addAll(List<T> items);

    @Override
    public abstract void update(String id, T item);
}