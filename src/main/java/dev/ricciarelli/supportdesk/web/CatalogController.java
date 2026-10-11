package dev.ricciarelli.supportdesk.web;

import dev.ricciarelli.supportdesk.application.CatalogService;
import dev.ricciarelli.supportdesk.web.model.CatalogModels.*;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {
    private final CatalogService service;

    public CatalogController(CatalogService service) {
        this.service = service;
    }

    @GetMapping("/departments")
    public List<Department> departments() {
        return service.departments();
    }

    @GetMapping("/categories")
    public List<Category> categories() {
        return service.categories();
    }

    @GetMapping("/users")
    public List<User> users() {
        return service.users();
    }

    @PostMapping("/departments")
    @ResponseStatus(HttpStatus.CREATED)
    public Department createDepartment(@Valid @RequestBody CreateDepartment request) {
        return service.createDepartment(request);
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public Category createCategory(@Valid @RequestBody CreateCategory request) {
        return service.createCategory(request);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@Valid @RequestBody CreateUser request) {
        return service.createUser(request);
    }
}
