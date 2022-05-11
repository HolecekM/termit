package cz.cvut.kbss.termit.service.changetracking;

import cz.cvut.kbss.changetracking.model.ChangeVector;
import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.MultilingualString;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.vocabulary.DC;
import cz.cvut.kbss.jopa.vocabulary.SKOS;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.util.HasIdentifier;
import cz.cvut.kbss.termit.persistence.DescriptorFactory;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static cz.cvut.kbss.termit.service.changetracking.MetamodelBasedChangeCalculatorTest.cloneOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional("jpaTxManager")
class ChangeTrackingServiceTest extends BaseServiceTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private javax.persistence.EntityManager jpaEm;

    @Autowired
    private DescriptorFactory descriptorFactory;

    @Autowired
    private ChangeTrackingService sut;

    private User author;

    private Vocabulary vocabulary;

    @BeforeEach
    void setUp() {
        this.author = Generator.generateUserWithId();
        Environment.setCurrentUser(author);
        this.vocabulary = Generator.generateVocabularyWithId();
        transactional(() -> {
            em.persist(author);
            em.persist(vocabulary, descriptorFactory.vocabularyDescriptor(vocabulary));
        });
    }

    private List<ChangeVector<?>> findRecords(HasIdentifier entity) {
        return sut.getAllForObject(entity.getClass().getAnnotation(OWLClass.class).iri(), entity.getUri().toString());
    }

    @Test
    void recordUpdateEventDoesNothingWhenAssetDidNotChange() {
        enableRdfsInference(em);
        final Term original = Generator.generateTermWithId();
        original.setVocabulary(vocabulary.getUri());
        transactional(() -> em.persist(original, descriptorFactory.termDescriptor(original)));

        final Term update = cloneOf(original);
        transactional(() -> sut.recordUpdateEvent(update, original));

        assertTrue(findRecords(original).isEmpty());
    }

    @Test
    void recordUpdateRecordsSingleChangeToLiteralAttribute() {
        enableRdfsInference(em);
        final Term original = Generator.generateTermWithId();
        original.setGlossary(vocabulary.getGlossary().getUri());
        transactional(() -> em.persist(original, descriptorFactory.termDescriptor(vocabulary)));

        final Term update = cloneOf(original);
        update.setDefinition(MultilingualString.create("Updated definition of this term.", Environment.LANGUAGE));
        transactional(() -> sut.recordUpdateEvent(update, original));


        final List<ChangeVector<?>> result = findRecords(original);
        assertEquals(1, result.size());
        final ChangeVector<?> vector = result.get(0);
        assertEquals(original.getUri().toString(), vector.getObjectId());
        assertEquals(SKOS.DEFINITION, vector.getAttributeName());
    }

    @Test
    void recordUpdateRecordsMultipleChangesToAttributes() {
        enableRdfsInference(em);
        final Term original = Generator.generateTermWithId();
        original.setGlossary(vocabulary.getGlossary().getUri());
        transactional(() -> em.persist(original, descriptorFactory.termDescriptor(vocabulary)));

        final Term update = cloneOf(original);
        update.setDefinition(MultilingualString.create("Updated definition of this term.", Environment.LANGUAGE));
        update.setSources(Collections.singleton(Generator.generateUri().toString()));
        transactional(() -> sut.recordUpdateEvent(update, original));

        final List<ChangeVector<?>> result = findRecords(original);
        assertEquals(2, result.size());
        result.forEach(record -> assertThat(
                record.getAttributeName(),
                anyOf(equalTo(SKOS.DEFINITION), equalTo(DC.Terms.SOURCE))
        ));
    }
}
