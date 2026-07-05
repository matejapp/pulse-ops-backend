package com.mateja.pulseops.checkresult.application;


import com.mateja.pulseops.httpmonitor.domain.HttpMonitor;
import com.mateja.pulseops.httpmonitor.persistence.HttpMonitorRepo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Service
public class CheckScheduler {

    private final HttpMonitorRepo httpMonitorRepo;
    private final MonitorProber monitorProber;
    private final ExecutorService executorService;

    public CheckScheduler(HttpMonitorRepo httpMonitorRepo, MonitorProber monitorProber,  ExecutorService executorService) {
        this.httpMonitorRepo = httpMonitorRepo;
        this.monitorProber = monitorProber;
        this.executorService = executorService;
    }

    @Scheduled(fixedRate = 60_000)
    public void runCheck(){
        List<HttpMonitor> monitors = httpMonitorRepo.findByEnabledTrue();

        List<Future<?>> futures = new ArrayList<>();
        for(HttpMonitor monitor : monitors){
            futures.add(executorService.submit(() -> monitorProber.probe(monitor)));
        }
        for(Future<?> future : futures){
            try{
                future.get(); //blocks until THIS task finishes
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e){}//Probe() handles its own errors; rare occurrence
        }
    }
}
