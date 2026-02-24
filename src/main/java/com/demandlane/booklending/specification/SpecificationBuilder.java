package com.demandlane.booklending.specification;

import java.lang.reflect.Field;

import org.springframework.data.jpa.domain.Specification;

public class SpecificationBuilder {

    /**
     * Builds a JPA Specification from a filter object (e.g., UserDto.Filter, BookDto.Filter).
     * Uses reflection to extract non-null field values from the filter and build specifications.
     * <p>
     * String fields → case-insensitive LIKE %value%
     * Enum fields   → exact match (value converted to uppercase)
     * Numeric fields → exact match
     * <p>
     * The {@code deletedAt IS NULL} soft-delete guard is always included.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Specification<T> fromFilter(Object filter, Class<T> entityClass) {
        // Always filter out soft-deleted records
        Specification<T> spec = (root, query, cb) -> cb.isNull(root.get("deletedAt"));

        if (filter == null) {
            return spec;
        }

        Field[] filterFields = filter.getClass().getDeclaredFields();
        for (Field filterField : filterFields) {
            try {
                filterField.setAccessible(true);
                Object value = filterField.get(filter);

                // Skip null or blank values
                if (value == null) continue;
                if (value instanceof String && ((String) value).isBlank()) continue;

                final String fieldName = filterField.getName();

                // Handle relationship ID fields (e.g., userId -> user.id, bookId -> book.id)
                final String actualFieldName;
                final boolean isRelationshipId;
                if (fieldName.endsWith("Id")) {
                    String relationshipName = fieldName.substring(0, fieldName.length() - 2);
                    Field relationshipField = findField(entityClass, relationshipName);
                    if (relationshipField != null) {
                        actualFieldName = relationshipName;
                        isRelationshipId = true;
                    } else {
                        actualFieldName = fieldName;
                        isRelationshipId = false;
                    }
                } else {
                    actualFieldName = fieldName;
                    isRelationshipId = false;
                }

                Field entityField = findField(entityClass, actualFieldName);
                if (entityField == null) continue;

                Class<?> entityFieldType = entityField.getType();

                if (isRelationshipId) {
                    final Long longValue = value instanceof Long ? (Long) value : Long.parseLong(value.toString());
                    spec = spec.and((root, query, cb) -> cb.equal(root.get(actualFieldName).get("id"), longValue));
                } else if (String.class.equals(entityFieldType)) {
                    final String stringValue = (String) value;
                    spec = spec.and((root, query, cb) ->
                            cb.like(cb.lower(root.get(actualFieldName)), "%" + stringValue.toLowerCase() + "%"));
                } else if (entityFieldType.isEnum()) {
                    try {
                        final Object enumValue = Enum.valueOf((Class<Enum>) entityFieldType, value.toString().toUpperCase());
                        spec = spec.and((root, query, cb) -> cb.equal(root.get(actualFieldName), enumValue));
                    } catch (IllegalArgumentException ignored) {
                        // unknown enum constant – skip silently
                    }
                } else if (Long.class.equals(entityFieldType) || long.class.equals(entityFieldType)) {
                    final Long longValue = value instanceof Long ? (Long) value : Long.parseLong(value.toString());
                    spec = spec.and((root, query, cb) -> cb.equal(root.get(actualFieldName), longValue));
                }
            } catch (IllegalAccessException e) {
                // Skip fields that cannot be accessed
            }
        }

        return spec;
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && !Object.class.equals(current)) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
