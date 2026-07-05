package com.mateja.pulseops.checkresult.web;

import com.mateja.pulseops.checkresult.application.CheckResultService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/monitors/{monitorId}/results")
public class CheckResultController {

    private final CheckResultService  checkResultService;

    public CheckResultController(CheckResultService checkResultService) {
        this.checkResultService = checkResultService;
    }

    @GetMapping
    public PagedModel<CheckResultResponse> history(@PathVariable UUID monitorId, @PageableDefault(size = 20, sort = "checkedAt", direction = Sort.Direction.DESC) Pageable pageable)
    {
        return checkResultService.getHistory(monitorId, pageable);
    }

}
