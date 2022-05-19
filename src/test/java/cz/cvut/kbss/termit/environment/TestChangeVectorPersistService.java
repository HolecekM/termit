package cz.cvut.kbss.termit.environment;

import cz.cvut.kbss.changetracking.model.ChangeVector;
import cz.cvut.kbss.changetracking.strategy.storage.StorageStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service used to manually persist ChangeVectors when testing the "reading" capabilities of the change tracker.
 */
@Service
public class TestChangeVectorPersistService {
    private final StorageStrategy storageStrategy;

    @Autowired
    public TestChangeVectorPersistService(StorageStrategy storageStrategy) {
        this.storageStrategy = storageStrategy;
    }

    public void persistChangeVectors(ChangeVector<?>... vectors) {
        storageStrategy.save(vectors);
    }
}
