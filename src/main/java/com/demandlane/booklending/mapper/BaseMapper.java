package com.demandlane.booklending.mapper;

public interface BaseMapper<T, R, S> {

    S toResponse(T entity);

    T toEntity(R request);
}
