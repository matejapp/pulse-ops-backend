package com.mateja.pulseops.httpmonitor.web;

import com.mateja.pulseops.httpmonitor.domain.HttpMethod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record CreateHttpMonitorRequest(
        @NotBlank(message = "Target url is required!")
        @Size(max = 2048)
        @URL(message = "Target url must be a valid URL")
        String targetUrl,
        // @NotNull, not @NotBlank: httpMethod is an enum, not a String. Jackson maps "GET"/"HEAD"
        // to the enum; if the JSON omits it (or sends an unknown value) the request is rejected
        // instead of persisting a null method.
        @NotNull(message = "Http method is required!")
        HttpMethod httpMethod,
        // A primitive int can't be null, so @NotNull would always pass and does nothing here.
        // Range constraints are what actually validate: reject anything outside real HTTP codes.
        @Min(value = 100, message = "Expected status must be a valid HTTP status")
        @Max(value = 599, message = "Expected status must be a valid HTTP status")
        int expectedStatus
) {
}
