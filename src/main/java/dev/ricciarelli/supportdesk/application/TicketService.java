package dev.ricciarelli.supportdesk.application;

import dev.ricciarelli.supportdesk.domain.TicketStatus;
import dev.ricciarelli.supportdesk.persistence.TicketRepository;
import dev.ricciarelli.supportdesk.web.error.DomainException;
import dev.ricciarelli.supportdesk.web.model.TicketModels.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
@Transactional(readOnly = true)
public class TicketService {
    private final TicketRepository repository;
    private final CatalogService catalog;
    private final Clock clock;

    public TicketService(TicketRepository repository, CatalogService catalog, Clock clock) {
        this.repository = repository;
        this.catalog = catalog;
        this.clock = clock;
    }

    public TicketPage list(TicketFilter filter) {
        return repository.list(filter);
    }

    public TicketDetail detail(long id) {
        return new TicketDetail(requireTicket(id), repository.history(id));
    }

    @Transactional
    public TicketDetail create(CreateTicket request) {
        catalog.requireRequester(request.requesterId());
        catalog.requireCategory(request.categoryId());
        OffsetDateTime now = OffsetDateTime.now(clock);
        long id = repository.create(request, now, now.plus(request.priority().resolutionWindow()));
        repository.addEvent(
                id,
                request.requesterId(),
                "CREATED",
                null,
                TicketStatus.OPEN,
                "Chamado aberto.",
                now);
        return detail(id);
    }

    @Transactional
    public TicketDetail assign(long id, Assignment request) {
        catalog.requireAgent(request.actorId());
        catalog.requireAgent(request.agentId());
        TicketView ticket = lockedTicket(id);
        if (ticket.status() == TicketStatus.CLOSED || ticket.status() == TicketStatus.RESOLVED) {
            throw DomainException.conflict(
                    "ASSIGNMENT_NOT_ALLOWED", "Reabra o chamado antes de alterar o responsável.");
        }
        if (ticket.assignedAgentId() != null && ticket.assignedAgentId() == request.agentId())
            return detail(id);
        OffsetDateTime now = OffsetDateTime.now(clock);
        repository.assign(id, request.agentId(), now);
        repository.addEvent(
                id,
                request.actorId(),
                "ASSIGNED",
                null,
                null,
                "Responsável atribuído: usuário " + request.agentId() + ".",
                now);
        return detail(id);
    }

    @Transactional
    public TicketDetail transition(long id, Transition request) {
        catalog.requireAgent(request.actorId());
        TicketView ticket = lockedTicket(id);
        if (!ticket.status().allows(request.status())) {
            throw DomainException.conflict(
                    "INVALID_TRANSITION",
                    "Transição de "
                            + ticket.status()
                            + " para "
                            + request.status()
                            + " não permitida.");
        }
        if (ticket.assignedAgentId() == null) {
            throw DomainException.conflict(
                    "ASSIGNEE_REQUIRED", "Atribua um responsável antes de alterar o status.");
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime resolvedAt =
                request.status() == TicketStatus.RESOLVED
                        ? now
                        : request.status() == TicketStatus.CLOSED ? ticket.resolvedAt() : null;
        OffsetDateTime closedAt = request.status() == TicketStatus.CLOSED ? now : null;
        repository.transition(id, request.status(), resolvedAt, closedAt, now);
        repository.addEvent(
                id,
                request.actorId(),
                "STATUS_CHANGED",
                ticket.status(),
                request.status(),
                request.message().trim(),
                now);
        return detail(id);
    }

    @Transactional
    public TicketDetail comment(long id, Comment request) {
        catalog.requireActiveUser(request.actorId());
        lockedTicket(id);
        repository.addEvent(
                id,
                request.actorId(),
                "COMMENT_ADDED",
                null,
                null,
                request.message().trim(),
                OffsetDateTime.now(clock));
        return detail(id);
    }

    @Transactional
    public void archive(long id, long actorId) {
        catalog.requireAgent(actorId);
        TicketView ticket = lockedTicket(id);
        if (ticket.status() != TicketStatus.CLOSED) {
            throw DomainException.conflict(
                    "CLOSED_REQUIRED", "Encerre o chamado antes de arquivá-lo.");
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        repository.archive(id, now);
        repository.addEvent(
                id,
                actorId,
                "ARCHIVED",
                null,
                null,
                "Chamado arquivado; histórico preservado.",
                now);
    }

    private TicketView requireTicket(long id) {
        return repository.find(id).orElseThrow(() -> DomainException.notFound("Chamado"));
    }

    private TicketView lockedTicket(long id) {
        if (!repository.lock(id)) throw DomainException.notFound("Chamado");
        return requireTicket(id);
    }
}
