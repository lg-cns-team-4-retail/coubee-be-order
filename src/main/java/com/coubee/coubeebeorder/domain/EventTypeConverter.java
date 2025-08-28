package com.coubee.coubeebeorder.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.stream.Stream;

@Converter(autoApply = true)
public class EventTypeConverter implements AttributeConverter<EventType, String> {

    @Override
    public String convertToDatabaseColumn(EventType eventType) {
        if (eventType == null) {
            return null;
        }
        // Ensure the value is always stored in uppercase in the database.
        return eventType.name();
    }

    @Override
    public EventType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        // Safely convert the database string to an Enum by ignoring case and trimming whitespace.
        // This correctly handles 'purchase', 'PURCHASE', or ' PURCHASE ' as EventType.PURCHASE.
        return Stream.of(EventType.values())
          .filter(c -> c.name().equalsIgnoreCase(dbData.trim()))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Unknown EventType value: " + dbData));
    }
}
