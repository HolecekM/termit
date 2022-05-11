package cz.cvut.kbss.termit.service.changetracking;

import cz.cvut.kbss.changetracking.ChangeTracker;
import cz.cvut.kbss.changetracking.model.JsonChangeVector;
import cz.cvut.kbss.changetracking.strategy.entity.JopaEntityStrategy;
import cz.cvut.kbss.changetracking.strategy.storage.JpaStorageStrategy;
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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Objects;

/**
 * Tracks changes to assets.
 */
@Service
@EntityScan("cz.cvut.kbss.changetracking.model")
@Transactional
public class ChangeTrackingService {
    private static final Logger LOG = LoggerFactory.getLogger(ChangeTrackingService.class);

    private final ChangeCalculator changeCalculator;

    private final ChangeRecordDao changeRecordDao;
    private final EntityManager jpaEm;
    private final EntityManagerFactory jpaEmf;
    private final ChangeTracker changeTracker;

    @Autowired
    public ChangeTrackingService(
            ChangeCalculator changeCalculator,
            ChangeRecordDao changeRecordDao,
            javax.persistence.EntityManager jpaEm,
            javax.persistence.EntityManagerFactory jpaEmf,
            cz.cvut.kbss.jopa.model.EntityManagerFactory jopaEmf
    ) {
        this.changeCalculator = changeCalculator;
        this.changeRecordDao = changeRecordDao;
        this.jpaEm = jpaEm;
        this.jpaEmf = jpaEmf;
        this.changeTracker = new ChangeTracker(new JopaEntityStrategy(jopaEmf.getMetamodel()), new JpaStorageStrategy(jpaEm));
    }

    /**
     * Records an asset addition to the repository.
     *
     * @param added The added asset
     */
    @Transactional
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
    @Transactional
    public void recordUpdateEvent(Asset<?> update, Asset<?> original) {
        EntityManagerFactory emf = this.jpaEmf;
        EntityManager em = this.jpaEm;
        /*System.out.println(em.getTransaction());
        System.out.println(em.isJoinedToTransaction());*/
        em.persist(new JsonChangeVector());
        final User user = SecurityUtils.currentUser().toUser();
        // set breakpoint on the line below -> step in -> step into StorageStrategy#save -> em#persist does nothing
        //  except look up new ID, em#getTransaction ISEs, #isJoinedToTransaction() is false BUT
        //  TransactionSynchronizationManager.isActualTransactionActive() is true
        changeTracker.compareAndSave(original, update, user.getUri().toString());
        // TODO: log
    }
}
