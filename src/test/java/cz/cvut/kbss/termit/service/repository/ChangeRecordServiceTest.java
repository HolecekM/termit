package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.changetracking.model.ChangeVector;
import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.vocabulary.DC;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.environment.TestChangeVectorPersistService;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.persistence.DescriptorFactory;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChangeRecordServiceTest extends BaseServiceTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private TestChangeVectorPersistService vectorPersistService;

    @Autowired
    private DescriptorFactory descriptorFactory;

    @Autowired
    private ChangeRecordService sut;

    private User author;

    private Vocabulary asset;

    @BeforeEach
    void setUp() {
        this.asset = Generator.generateVocabularyWithId();
        this.author = Generator.generateUserWithId();
        Environment.setCurrentUser(author);
        transactional(() -> {
            em.persist(author);
            em.persist(asset, descriptorFactory.vocabularyDescriptor(asset));
        });
    }

    @Test
    void getChangesReturnsChangesForSpecifiedAsset() {
        enableRdfsInference(em);
        final List<ChangeVector<?>> records = generateChanges();
        records.sort(Comparator.comparing(v -> ((ChangeVector<?>)v).getTimestamp()).reversed());

        final List<ChangeVector<?>> result = sut.getChanges(asset);
        assertEquals(records, result);
    }

    private List<ChangeVector<?>> generateChanges() {
        final List<ChangeVector<?>> records = IntStream.range(0, 5).mapToObj(i -> {
            final ChangeVector<?> r = new ChangeVector<>(asset.getClass().getAnnotation(OWLClass.class).iri(),
                    asset.getUri().toString(),
                    DC.Terms.TITLE, asset.getLabel(), Instant.ofEpochMilli(System.currentTimeMillis() - i * 10000L));
            r.setAuthorId(author.getUri().toString());
            return r;
        }).collect(Collectors.toList());
        jpaTransactional(() -> vectorPersistService.persistChangeVectors(records.toArray(ChangeVector[]::new)));
        return records;
    }
}
