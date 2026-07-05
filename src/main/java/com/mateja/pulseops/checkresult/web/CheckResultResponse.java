package com.mateja.pulseops.checkresult.web;

import com.mateja.pulseops.checkresult.domain.CheckResult;

import java.time.Instant;
import java.util.UUID;

public record CheckResultResponse (UUID checkResultId, UUID monitorId, boolean success, Integer responseStatus, int latencyMs, String errorMessage, Instant checkedAt) {

        public static CheckResultResponse fromEntity(CheckResult entity){
            if(entity == null)  return null;

            return new CheckResultResponse(entity.getCheckResultId(), entity.getMonitor().getMonitorId(),
                    entity.isSuccess(),entity.getResponseStatus(),entity.getLatencyMs(),entity.getErrorMessage(),entity.getCheckedAt());
        }

}
