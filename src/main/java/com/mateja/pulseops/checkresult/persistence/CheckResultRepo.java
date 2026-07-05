package com.mateja.pulseops.checkresult.persistence;

import com.mateja.pulseops.checkresult.domain.CheckResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CheckResultRepo  extends JpaRepository<CheckResult, UUID>
{

    Page<CheckResult> findByMonitor_MonitorId(UUID monitorId, Pageable pageable);
}
