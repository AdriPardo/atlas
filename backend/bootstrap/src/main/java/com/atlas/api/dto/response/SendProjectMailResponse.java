package com.atlas.api.dto.response;

public record SendProjectMailResponse(boolean sent, String detail, int remainingToday) {}
