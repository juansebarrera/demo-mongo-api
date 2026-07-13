package com.example.demo_mongo_api.controller.dto;

import java.util.List;

public record BulkResponse(
        int totalRecibidos,
        int insertados,
        int fallidos,
        List<String> ids,
        List<BulkError> errores
) {}
