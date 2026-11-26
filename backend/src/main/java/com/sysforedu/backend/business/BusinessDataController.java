package com.sysforedu.backend.business;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data/{domain}")
public class BusinessDataController {
    private final BusinessDataService service;

    public BusinessDataController(BusinessDataService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> list(@PathVariable String domain) {
        return service.list(domain);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@PathVariable String domain, @RequestBody Map<String, Object> record) {
        return service.create(domain, record);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String domain, @PathVariable String id,
                                      @RequestBody Map<String, Object> record) {
        return service.update(domain, id, record);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String domain, @PathVariable String id) {
        service.delete(domain, id);
    }
}
