package dev.ricciarelli.supportdesk.web;

import dev.ricciarelli.supportdesk.application.TicketService;
import dev.ricciarelli.supportdesk.domain.Priority;
import dev.ricciarelli.supportdesk.domain.TicketStatus;
import dev.ricciarelli.supportdesk.web.model.TicketModels.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@Validated
@RequestMapping("/api/v1/tickets")
public class TicketController {
    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TicketDetail> create(@Valid @RequestBody CreateTicket request) {
        TicketDetail detail = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/tickets/" + detail.ticket().id()))
                .body(detail);
    }

    @GetMapping
    public TicketPage list(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(required = false) @Positive Long agentId,
            @RequestParam(required = false, name = "q") @Size(max = 100) String query,
            @RequestParam(defaultValue = "0") @Min(0) @Max(10000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(
                new TicketFilter(status, priority, categoryId, agentId, query, page, size));
    }

    @GetMapping("/{id}")
    public TicketDetail detail(@PathVariable @Positive long id) {
        return service.detail(id);
    }

    @PatchMapping("/{id}/assignment")
    public TicketDetail assign(
            @PathVariable @Positive long id, @Valid @RequestBody Assignment request) {
        return service.assign(id, request);
    }

    @PatchMapping("/{id}/status")
    public TicketDetail transition(
            @PathVariable @Positive long id, @Valid @RequestBody Transition request) {
        return service.transition(id, request);
    }

    @PostMapping("/{id}/comments")
    public TicketDetail comment(
            @PathVariable @Positive long id, @Valid @RequestBody Comment request) {
        return service.comment(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(
            @PathVariable @Positive long id, @RequestParam @Positive long actorId) {
        service.archive(id, actorId);
        return ResponseEntity.noContent().build();
    }
}
