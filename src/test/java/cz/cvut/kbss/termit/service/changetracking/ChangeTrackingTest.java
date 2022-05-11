package cz.cvut.kbss.termit.service.changetracking;

import cz.cvut.kbss.changetracking.model.ChangeVector;
import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.MultilingualString;
import cz.cvut.kbss.jopa.vocabulary.DC;
import cz.cvut.kbss.jopa.vocabulary.SKOS;
import cz.cvut.kbss.termit.dto.TermStatus;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Asset;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.changetracking.UpdateChangeRecord;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.persistence.DescriptorFactory;
import cz.cvut.kbss.termit.service.repository.ResourceRepositoryService;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
import cz.cvut.kbss.termit.service.repository.VocabularyRepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class ChangeTrackingTest extends AbstractChangeTrackingTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private DescriptorFactory descriptorFactory;

    @Autowired
    private VocabularyRepositoryService vocabularyService;

    @Autowired
    private TermRepositoryService termService;

    @Autowired
    private ChangeTrackingService changeTrackingService;

    @Autowired
    private ResourceRepositoryService resourceService;

    private User author;

    private Vocabulary vocabulary;

    List<ChangeVector<?>> getAll(Asset<?> asset) {
        return changeTrackingService.getAllForObject(
                cz.cvut.kbss.termit.util.Vocabulary.s_c_slovnik,
                asset.getUri().toString()
        );
    }

    @BeforeEach
    void setUp() {
        this.author = Generator.generateUserWithId();
        Environment.setCurrentUser(author);
        this.vocabulary = Generator.generateVocabularyWithId();
        transactional(() -> em.persist(author));
    }

    @Test
    void updatingVocabularyLiteralAttributeCreatesUpdateChangeRecord() {
        enableRdfsInference(em);
        transactional(() -> em.persist(vocabulary, descriptorFactory.vocabularyDescriptor(vocabulary)));
        vocabulary.setLabel("Updated vocabulary label");
        transactional(() -> vocabularyService.update(vocabulary));

        final List<ChangeVector<?>> result = getAll(vocabulary);
        assertEquals(1, result.size());
        assertEquals(vocabulary.getUri(), result.get(0).getObjectId());
        assertEquals(DC.Terms.TITLE, result.get(0).getAttributeName());
    }

    @Test
    void updatingVocabularyReferenceAndLiteralAttributesCreatesTwoUpdateRecords() {
        enableRdfsInference(em);
        final Vocabulary imported = Generator.generateVocabularyWithId();
        transactional(() -> {
            em.persist(imported, descriptorFactory.vocabularyDescriptor(imported));
            em.persist(vocabulary, descriptorFactory.vocabularyDescriptor(vocabulary));
        });
        vocabulary.setLabel("Updated vocabulary label");
        vocabulary.setImportedVocabularies(Collections.singleton(imported.getUri()));
        transactional(() -> vocabularyService.update(vocabulary));

        final List<ChangeVector<?>> result = getAll(vocabulary);
        assertEquals(2, result.size());
        result.forEach(chr -> {
            assertEquals(vocabulary.getUri(), chr.getObjectId());
            assertThat(result.get(0), instanceOf(UpdateChangeRecord.class));
            assertThat(chr.getAttributeName(), anyOf(equalTo(DC.Terms.TITLE),
                                                                                          equalTo(cz.cvut.kbss.termit.util.Vocabulary.s_p_importuje_slovnik)));
        });
    }

    @Test
    void updatingTermLiteralAttributeCreatesChangeRecord() {
        enableRdfsInference(em);
        final Term term = Generator.generateTermWithId();
        transactional(() -> {
            em.persist(vocabulary, descriptorFactory.vocabularyDescriptor(vocabulary));
            term.setGlossary(vocabulary.getGlossary().getUri());
            em.persist(term, descriptorFactory.termDescriptor(vocabulary));
            Generator.addTermInVocabularyRelationship(term, vocabulary.getUri(), em);
        });
        term.setDefinition(MultilingualString.create("Updated term definition.", Environment.LANGUAGE));
        // This is normally inferred
        term.setVocabulary(vocabulary.getUri());
        transactional(() -> termService.update(term));

        final List<ChangeVector<?>> result = getAll(term);
        assertEquals(1, result.size());
        assertEquals(term.getUri(), result.get(0).getObjectId());
        assertEquals(SKOS.DEFINITION, result.get(0).getAttributeName());
    }

    @Test
    void updatingTermReferenceAttributeCreatesChangeRecord() {
        enableRdfsInference(em);
        final Term parent = Generator.generateTermWithId();
        final Term term = Generator.generateTermWithId();
        transactional(() -> {
            em.persist(vocabulary, descriptorFactory.vocabularyDescriptor(vocabulary));
            term.setGlossary(vocabulary.getGlossary().getUri());
            parent.setGlossary(vocabulary.getGlossary().getUri());
            em.persist(parent, descriptorFactory.termDescriptor(vocabulary));
            em.persist(term, descriptorFactory.termDescriptor(vocabulary));
            Generator.addTermInVocabularyRelationship(parent, vocabulary.getUri(), em);
            Generator.addTermInVocabularyRelationship(term, vocabulary.getUri(), em);
        });
        term.addParentTerm(parent);
        // This is normally inferred
        term.setVocabulary(vocabulary.getUri());
        transactional(() -> termService.update(term));

        final List<ChangeVector<?>> result = getAll(term);
        assertEquals(1, result.size());
        assertEquals(term.getUri(), result.get(0).getObjectId());
        assertEquals(SKOS.BROADER, result.get(0).getAttributeName());
    }

    @Test
    void updatingTermLiteralAttributesCreatesChangeRecordWithOriginalAndNewValue() {
        enableRdfsInference(em);
        final Term term = Generator.generateTermWithId();
        final MultilingualString originalDefinition = term.getDefinition();
        transactional(() -> {
            em.persist(vocabulary, descriptorFactory.vocabularyDescriptor(vocabulary));
            term.setGlossary(vocabulary.getGlossary().getUri());
            em.persist(term, descriptorFactory.termDescriptor(vocabulary));
            Generator.addTermInVocabularyRelationship(term, vocabulary.getUri(), em);
        });
        final MultilingualString newDefinition = MultilingualString
                .create("Updated term definition.", Environment.LANGUAGE);
        term.setDefinition(newDefinition);
        // This is normally inferred
        term.setVocabulary(vocabulary.getUri());
        transactional(() -> termService.update(term));

        final List<ChangeVector<?>> result = getAll(term);
        assertEquals(1, result.size());
        assertEquals(Collections.singleton(originalDefinition),
                     result.get(0).getPreviousValue());
        //assertEquals(Collections.singleton(newDefinition), result.get(0).getNewValue());
    }

    @Test
    void updatingTermReferenceAttributeCreatesChangeRecordWithOriginalAndNewValue() {
        enableRdfsInference(em);
        final Term parent = Generator.generateTermWithId();
        final Term term = Generator.generateTermWithId();
        transactional(() -> {
            em.persist(vocabulary, descriptorFactory.vocabularyDescriptor(vocabulary));
            parent.setGlossary(vocabulary.getGlossary().getUri());
            term.setGlossary(vocabulary.getGlossary().getUri());
            em.persist(parent, descriptorFactory.termDescriptor(vocabulary));
            em.persist(term, descriptorFactory.termDescriptor(vocabulary));
            Generator.addTermInVocabularyRelationship(parent, vocabulary.getUri(), em);
            Generator.addTermInVocabularyRelationship(term, vocabulary.getUri(), em);
        });
        term.addParentTerm(parent);
        // This is normally inferred
        term.setVocabulary(vocabulary.getUri());
        transactional(() -> termService.update(term));

        final List<ChangeVector<?>> result = getAll(term);
        assertFalse(result.isEmpty());
        assertNull(result.get(0).getPreviousValue());
        //assertEquals(Collections.singleton(parent.getUri()), result.get(0).getNewValue());
    }

    @Test
    void persistingFileDoesNotCreatePersistChangeRecord() {
        enableRdfsInference(em);
        final File file = Generator.generateFileWithId("test.html");
        transactional(() -> resourceService.persist(file));

        final List<ChangeVector<?>> result = getAll(file);
        assertEquals(0, result.size());
    }

    @Test
    void updatingTermDraftStatusCreatesUpdateChangeRecord() {
        enableRdfsInference(em);
        enableRdfsInference(em);
        final Term term = Generator.generateTermWithId();
        transactional(() -> {
            em.persist(vocabulary, descriptorFactory.vocabularyDescriptor(vocabulary));
            term.setGlossary(vocabulary.getGlossary().getUri());
            em.persist(term, descriptorFactory.termDescriptor(vocabulary));
            Generator.addTermInVocabularyRelationship(term, vocabulary.getUri(), em);
        });

        termService.setStatus(term, TermStatus.CONFIRMED);
        final List<ChangeVector<?>> result = getAll(term);
        assertEquals(1, result.size());
        assertEquals(cz.cvut.kbss.termit.util.Vocabulary.s_p_je_draft, result.get(0).getAttributeName());
    }
}
