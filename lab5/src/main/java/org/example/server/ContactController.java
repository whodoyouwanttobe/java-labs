package org.example.server;

import org.example.api.ContactNotFoundException;
import org.example.model.Contact;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService service;

    public ContactController(ContactService service) {
        this.service = service;
    }

    @GetMapping
    public Contact[] list(@RequestParam(name = "prefix", required = false) String prefix,
                          @RequestParam(name = "limit", required = false) Integer limit) {
        return service.findAll(prefix == null ? "" : prefix, limit == null ? 0 : limit);
    }

    @GetMapping("/{id}")
    public Contact one(@PathVariable("id") String id) {
        return service.find(id)
                .orElseThrow(() -> new ContactNotFoundException("Контакт не найден: " + id));
    }
}
