package cz.cvut.kbss.termit.service.changetracking;

import cz.cvut.kbss.changetracking.ChangeTracker;
import cz.cvut.kbss.changetracking.model.ChangeVector;
import cz.cvut.kbss.changetracking.strategy.entity.JopaEntityStrategy;
import cz.cvut.kbss.changetracking.strategy.storage.StorageStrategy;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.termit.model.Asset;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.util.HasIdentifier;
import cz.cvut.kbss.termit.service.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Tracks changes to assets.
 */
@Service
@EntityScan("cz.cvut.kbss.changetracking.model")
@Transactional("jpaTxManager")
public class ChangeTrackingService {
    private final ChangeTracker changeTracker;

    @Autowired
    public ChangeTrackingService(
        cz.cvut.kbss.jopa.model.EntityManagerFactory jopaEmf,
        StorageStrategy storageStrategy
    ) {
        this.changeTracker = new ChangeTracker(new JopaEntityStrategy(jopaEmf.getMetamodel()), storageStrategy);
    }

    /**
     * Records an asset addition to the repository.
     *
     * @param added The added asset
     */
    public void recordAddEvent(Asset<?> added) {
        Objects.requireNonNull(added);
        // TODO
    }

    /**
     * Records an asset update.
     * <p>
     * Each changed attribute is stored as a separate change record
     *
     * @param update   The updated version of the asset
     * @param original The original version of the asset
     */
    public void recordUpdateEvent(Asset<?> update, Asset<?> original) {
        final User user = SecurityUtils.currentUser().toUser();
        changeTracker.compareAndSave(original, update, user.getUri().toString());
        // TODO: log
    }

    /**
     * @see ChangeTracker#getAllForObject(String, String)
     */
    public List<ChangeVector<?>> getAllForObject(HasIdentifier entity) {
        return changeTracker.getAllForObject(
                entity.getClass().getAnnotation(OWLClass.class).iri(),
                entity.getUri().toString()
        );
    }

    public List<ChangeVector<?>> getLastNChangedEntitiesOfType(URI entityType, int limit) {
        return changeTracker
                .getChangesOfTypeSince(Instant.EPOCH, entityType.toString())
                .stream()
                .filter(distinctByChangedEntityId())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<ChangeVector<?>> getLastNChangedEntitiesOfTypeForUser(URI entityType, User user, int limit) {
        return changeTracker
                .getChangesOfTypeSince(Instant.EPOCH, entityType.toString())
                .stream()
                .filter(vector -> vector.getAuthorId().equals(user.getUri().toString()))
                .filter(distinctByChangedEntityId())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private static Predicate<ChangeVector<?>> distinctByChangedEntityId() {
        Set<String> seen = ConcurrentHashMap.newKeySet();
        return vector -> seen.add(vector.getObjectId());
    }
}
