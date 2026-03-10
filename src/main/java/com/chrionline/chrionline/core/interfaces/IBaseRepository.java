package com.chrionline.chrionline.core.interfaces;


import java.util.List;

public interface IBaseRepository<T> {

    void add(T item);

    void addAll(List<T> items);

    void update(String id, T item);

    void delete(String id);

    T getById(String id);

    List<T> getAll();

    void clear();
}
