package org.example.server;

import org.example.api.ContactAlreadyExistsException;
import org.example.api.ContactNotFoundException;
import org.example.api.TreeNode;
import org.example.model.Contact;
import org.example.store.ContactStore;
import org.example.store.DuplicateKeyException;
import org.example.store.TrieNodeView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    private final ContactStore store;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public ContactService(ContactStore store) {
        this.store = store;
    }

    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    public boolean isEmpty() {
        return store.size() == 0;
    }

    public int size() {
        return store.size();
    }

    public int count(String prefix) {
        return store.countByPrefix(prefix);
    }

    public int nodeCount() {
        return store.nodeCount();
    }

    public Contact[] findAll(String prefix, int limit) {
        int size = store.size();
        if (size == 0) {
            return new Contact[0];
        }
        int max = limit <= 0 || limit > size ? size : limit;
        return store.findByPrefix(prefix, max);
    }

    public Contact[] all() {
        return store.toArray();
    }

    public Optional<Contact> find(String name) {
        return Optional.ofNullable(store.find(name));
    }

    public TreeNode snapshot(String prefix, int maxNodes) {
        if (maxNodes < 1) {
            return null;
        }
        TrieNodeView view = store.snapshot(prefix, maxNodes);
        return view == null ? null : toTreeNode(view);
    }

    public void add(Contact contact) {
        try {
            store.add(contact);
        } catch (DuplicateKeyException e) {
            throw new ContactAlreadyExistsException(e.getMessage());
        }
        fireChanged();
    }

    public void addAll(Contact[] contacts) {
        int added = 0;
        for (Contact contact : contacts) {
            try {
                store.add(contact);
                added++;
            } catch (DuplicateKeyException e) {
                log.debug("Пропущен дубликат: {}", contact.getName());
            }
        }
        if (added > 0) {
            fireChanged();
        }
    }

    public void update(String oldKey, Contact contact) {
        try {
            store.update(oldKey, contact);
        } catch (DuplicateKeyException e) {
            throw new ContactAlreadyExistsException(e.getMessage());
        }
        fireChanged();
    }

    public void remove(String name) {
        store.remove(name);
        fireChanged();
    }

    public void clear() {
        removeAll();
        fireChanged();
    }

    public void replaceAll(Contact[] contacts) {
        removeAll();
        for (Contact contact : contacts) {
            store.add(contact);
        }
        fireChanged();
    }

    private void removeAll() {
        for (Contact contact : store.toArray()) {
            store.remove(contact.getName());
        }
    }

    private void fireChanged() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (RuntimeException e) {
                log.warn("Слушатель изменений упал", e);
            }
        }
    }

    private static TreeNode toTreeNode(TrieNodeView view) {
        TrieNodeView[] children = view.children;
        TreeNode[] mapped = new TreeNode[children.length];
        for (int i = 0; i < children.length; i++) {
            mapped[i] = children[i] == null ? null : toTreeNode(children[i]);
        }
        return new TreeNode(view.symbol, view.hasValue, view.count, mapped);
    }
}
