package dev.ricciarelli.supportdesk.web;

import dev.ricciarelli.supportdesk.persistence.ReportRepository;
import dev.ricciarelli.supportdesk.web.model.ReportModels.*;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Transactional(readOnly = true)
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final ReportRepository repository;

    public ReportController(ReportRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/overview")
    public Overview overview() {
        return repository.overview();
    }

    @GetMapping("/by-category")
    public List<CategoryReport> byCategory() {
        return repository.byCategory();
    }

    @GetMapping("/by-agent")
    public List<AgentReport> byAgent() {
        return repository.byAgent();
    }

    @GetMapping("/sla")
    public List<SlaReport> sla() {
        return repository.sla();
    }
}
