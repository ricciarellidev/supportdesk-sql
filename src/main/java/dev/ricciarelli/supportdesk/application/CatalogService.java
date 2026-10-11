package dev.ricciarelli.supportdesk.application;

import dev.ricciarelli.supportdesk.domain.UserRole;
import dev.ricciarelli.supportdesk.persistence.CatalogRepository;
import dev.ricciarelli.supportdesk.web.error.DomainException;
import dev.ricciarelli.supportdesk.web.model.CatalogModels.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final CatalogRepository repository;

    public CatalogService(CatalogRepository repository) {
        this.repository = repository;
    }

    public List<Department> departments() {
        return repository.departments();
    }

    public List<Category> categories() {
        return repository.categories();
    }

    public List<User> users() {
        return repository.users();
    }

    @Transactional
    public Department createDepartment(CreateDepartment request) {
        return repository.createDepartment(request.name().trim());
    }

    @Transactional
    public Category createCategory(CreateCategory request) {
        requireDepartment(request.departmentId());
        return repository.createCategory(request.name().trim(), request.departmentId());
    }

    @Transactional
    public User createUser(CreateUser request) {
        requireDepartment(request.departmentId());
        return repository.createUser(request);
    }

    public void requireDepartment(long id) {
        if (!repository.departmentExists(id)) throw DomainException.notFound("Departamento");
    }

    public void requireCategory(long id) {
        if (!repository.categoryExists(id)) throw DomainException.notFound("Categoria");
    }

    public User requireActiveUser(long id) {
        User user = repository.findUser(id).orElseThrow(() -> DomainException.notFound("Usuário"));
        if (!user.active())
            throw DomainException.conflict("INACTIVE_USER", "O usuário está inativo.");
        return user;
    }

    public User requireAgent(long id) {
        User user = requireActiveUser(id);
        if (user.role() != UserRole.AGENT)
            throw DomainException.conflict("AGENT_REQUIRED", "Informe um usuário do tipo AGENT.");
        return user;
    }

    public User requireRequester(long id) {
        User user = requireActiveUser(id);
        if (user.role() != UserRole.REQUESTER)
            throw DomainException.conflict(
                    "REQUESTER_REQUIRED", "Informe um usuário do tipo REQUESTER.");
        return user;
    }
}
