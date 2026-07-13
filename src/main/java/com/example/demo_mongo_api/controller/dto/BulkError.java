package com.example.demo_mongo_api.controller.dto;

public record BulkError(int index, String campo, String mensaje) {}
