package com.mateja.pulseops.checkresult.application;

import com.mateja.pulseops.checkresult.domain.CheckResult;
import com.mateja.pulseops.httpmonitor.domain.HttpMonitor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;

@Service
public class MonitorProber {

    private final RestClient restClient;
    private final CheckResultWriter checkResultWriter;

    public MonitorProber(RestClient restClient, CheckResultWriter checkResultWriter) {
        this.restClient = restClient;
        this.checkResultWriter = checkResultWriter;
    }

    public CheckResult probe(HttpMonitor monitor)
    {
        long startTime = System.nanoTime();
        Integer statusCode = null;
        boolean isSuccess = false;
        String errorMessage = null;
        Instant checkStartTime = Instant.now();

        try{
            ResponseEntity<Void> response = restClient.method(HttpMethod.valueOf(monitor.getHttpMethod().name()))
                    .uri(monitor.getTargetUrl())
                    .retrieve()
                    .onStatus(status -> true, (request, res) -> {})
                    .toBodilessEntity();

            statusCode = response.getStatusCode().value();
            isSuccess = (statusCode == monitor.getExpectedStatus());
            if(!isSuccess){
                errorMessage = "expected " +  monitor.getExpectedStatus() + " got " + statusCode;
            }
        } catch (RestClientResponseException e){
            statusCode = e.getStatusCode().value();
            errorMessage = e.getStatusText();
        } catch (Exception e){
            errorMessage = e.getMessage();
        }

        long latency = (System.nanoTime() - startTime) / 1_000_000; //ms

        CheckResult result = new CheckResult(monitor,isSuccess, statusCode, (int)latency, errorMessage, checkStartTime );
        if(isSuccess){
            monitor.recordSuccess();
        }else {
            monitor.recordFailure();
        }

        checkResultWriter.record(monitor, result);
        return result;
    }
}

