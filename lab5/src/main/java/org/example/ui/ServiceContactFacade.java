package org.example.ui;

import org.example.api.ContactFacade;
import org.example.api.TreeNode;
import org.example.model.Contact;
import org.example.server.ContactService;

import java.util.Optional;

public class ServiceContactFacade implements ContactFacade {

    private final ContactService service;

    public ServiceContactFacade(ContactService service) {
        this.service = service;
    }

    @Override
    public boolean isEmpty() {
        return service.isEmpty();
    }

    @Override
    public int size() {
        return service.size();
    }

    @Override
    public int count(String prefix) {
        return service.count(prefix);
    }

    @Override
    public int nodeCount() {
        return service.nodeCount();
    }

    @Override
    public Contact[] findAll(String prefix) {
        return service.findAll(prefix, 0);
    }

    @Override
    public Contact[] all() {
        return service.all();
    }

    @Override
    public Optional<Contact> find(String name) {
        return service.find(name);
    }

    @Override
    public TreeNode snapshot(String prefix, int maxNodes) {
        return service.snapshot(prefix, maxNodes);
    }

    @Override
    public void add(Contact contact) {
        service.add(contact);
    }

    @Override
    public void update(String oldKey, Contact contact) {
        service.update(oldKey, contact);
    }

    @Override
    public void remove(String name) {
        service.remove(name);
    }

    @Override
    public void clear() {
        service.clear();
    }

    @Override
    public void replaceAll(Contact[] contacts) {
        service.replaceAll(contacts);
    }

    @Override
    public void addChangeListener(Runnable listener) {
        service.addChangeListener(listener);
    }
}
