package com.crypto.alerting.commons.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AppUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    // Add other utility methods here as needed
}
