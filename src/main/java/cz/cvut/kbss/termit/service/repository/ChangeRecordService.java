package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.changetracking.model.ChangeVector;
import cz.cvut.kbss.termit.model.Asset;
import cz.cvut.kbss.termit.service.changetracking.ChangeRecordProvider;
import cz.cvut.kbss.termit.service.changetracking.ChangeTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ChangeRecordService implements ChangeRecordProvider<Asset<?>> {

    private final ChangeTrackingService changeTrackingService;

    @Autowired
    public ChangeRecordService(ChangeTrackingService changeTrackingService) {
        this.changeTrackingService = changeTrackingService;
    }

    @Override
    public List<ChangeVector<?>> getChanges(Asset<?> asset) {
        Objects.requireNonNull(asset);
        return changeTrackingService.getAllForObject(asset);
    }
}
