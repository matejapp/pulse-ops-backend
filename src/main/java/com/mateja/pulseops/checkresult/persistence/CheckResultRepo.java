package com.mateja.pulseops.checkresult.persistence;

import com.mateja.pulseops.checkresult.domain.CheckResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CheckResultRepo  extends JpaRepository<CheckResult, UUID>
{

}
