package cz.cvut.kbss.termit.service.changetracking;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.kbss.changetracking.ChangeTracker;
import cz.cvut.kbss.changetracking.model.ChangeVector;
import cz.cvut.kbss.changetracking.strategy.entity.JopaEntityStrategy;
import cz.cvut.kbss.changetracking.strategy.storage.JpaStorageStrategy;
import cz.cvut.kbss.changetracking.strategy.storage.StorageStrategy;
import cz.cvut.kbss.termit.config.WebAppConfig;
import cz.cvut.kbss.termit.model.Asset;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.changetracking.AbstractChangeRecord;
import cz.cvut.kbss.termit.model.changetracking.PersistChangeRecord;
import cz.cvut.kbss.termit.persistence.dao.changetracking.ChangeRecordDao;
import cz.cvut.kbss.termit.service.security.SecurityUtils;
import cz.cvut.kbss.termit.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Tracks changes to assets.
 */
@Service
@EntityScan("cz.cvut.kbss.changetracking.model")
@Transactional("jpaTxManager")
public class ChangeTrackingService {
    private static final Logger LOG = LoggerFactory.getLogger(ChangeTrackingService.class);

    private final ChangeRecordDao changeRecordDao;
    private final ChangeTracker changeTracker;

    @Autowired
    public ChangeTrackingService(
            ChangeRecordDao changeRecordDao,
            javax.persistence.EntityManager jpaEm,
            cz.cvut.kbss.jopa.model.EntityManagerFactory jopaEmf
    ) {
        this.changeRecordDao = changeRecordDao;
        ObjectMapper mapper = WebAppConfig.createJsonObjectMapper();
        StorageStrategy storageStrategy = new JpaStorageStrategy(jpaEm, mapper);
        this.changeTracker = new ChangeTracker(new JopaEntityStrategy(jopaEmf.getMetamodel()), storageStrategy);
    }

    /**
     * Records an asset addition to the repository.
     *
     * @param added The added asset
     */
    public void recordAddEvent(Asset<?> added) {
        Objects.requireNonNull(added);
        final AbstractChangeRecord changeRecord = new PersistChangeRecord(added);
        changeRecord.setAuthor(SecurityUtils.currentUser().toUser());
        changeRecord.setTimestamp(Utils.timestamp());
        changeRecordDao.persist(changeRecord, added);
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
    public List<ChangeVector<?>> getAllForObject(String objectType, String objectId) {
        return changeTracker.getAllForObject(objectType, objectId);
    }
}
