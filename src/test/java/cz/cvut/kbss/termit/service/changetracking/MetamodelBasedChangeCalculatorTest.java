package cz.cvut.kbss.termit.service.changetracking;

import cz.cvut.kbss.changetracking.model.ChangeVector;
import cz.cvut.kbss.changetracking.strategy.entity.EntityStrategy;
import cz.cvut.kbss.changetracking.strategy.entity.JopaEntityStrategy;
import cz.cvut.kbss.jopa.model.EntityManagerFactory;
import cz.cvut.kbss.jopa.model.MultilingualString;
import cz.cvut.kbss.jopa.vocabulary.DC;
import cz.cvut.kbss.jopa.vocabulary.RDF;
import cz.cvut.kbss.jopa.vocabulary.SKOS;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Glossary;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.resource.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class MetamodelBasedChangeCalculatorTest extends AbstractChangeTrackingTest {

    @Autowired
    MetamodelBasedChangeCalculatorTest(EntityManagerFactory emf) {
        sut = new JopaEntityStrategy(emf.getMetamodel());
    }

    private final EntityStrategy sut;

    @Test
    void calculateChangesDiscoversChangeInSingularLiteralAttribute() {
        final Vocabulary original = Generator.generateVocabularyWithId();
        original.getModel().setUri(Generator.generateUri());
        original.getGlossary().setUri(Generator.generateUri());
        final Vocabulary changed = cloneOf(original);
        changed.setLabel("Updated label");

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        assertEquals(original.getUri().toString(), record.getObjectId());
        assertEquals(DC.Terms.TITLE, record.getAttributeName());
    }

    private static Vocabulary cloneOf(Vocabulary original) {
        final Vocabulary clone = new Vocabulary();
        clone.setUri(original.getUri());
        clone.setDescription(original.getDescription());
        clone.setModel(original.getModel());
        clone.setGlossary(original.getGlossary());
        clone.setLabel(original.getLabel());
        return clone;
    }

    @Test
    void calculateChangesDiscoversInSingularReferenceAttribute() {
        // Note: This does not normally happen, it simulates possible changes in model when assets would have singular references to other objects
        final Vocabulary original = Generator.generateVocabularyWithId();
        original.getModel().setUri(Generator.generateUri());
        original.getGlossary().setUri(Generator.generateUri());
        final Vocabulary changed = cloneOf(original);
        changed.setGlossary(new Glossary());
        changed.getGlossary().setUri(Generator.generateUri());

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        assertEquals(original.getUri().toString(), record.getObjectId());
        assertEquals(cz.cvut.kbss.termit.util.Vocabulary.s_p_ma_glosar, record.getAttributeName());
    }

    @Test
    void calculateChangesDiscoversChangeInPluralLiteralAttribute() {
        final Term original = Generator.generateTermWithId();
        final Term changed = cloneOf(original);
        changed.setSources(IntStream.range(0, 5).mapToObj(i -> "http://source" + i).collect(Collectors.toSet()));

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        assertEquals(original.getUri().toString(), record.getObjectId());
        assertEquals(DC.Terms.SOURCE, record.getAttributeName());
    }

    static Term cloneOf(Term original) {
        final Term clone = new Term();
        clone.setUri(original.getUri());
        clone.setLabel(new MultilingualString(original.getLabel().getValue()));
        clone.setAltLabels(original.getAltLabels());
        clone.setHiddenLabels(original.getHiddenLabels());
        clone.setSources(original.getSources());
        clone.setDefinition(new MultilingualString(original.getDefinition().getValue()));
        clone.setDescription(original.getDescription());
        clone.setVocabulary(original.getVocabulary());
        clone.setGlossary(original.getGlossary());
        return clone;
    }

    @Test
    void calculateChangesDiscoversChangesInPluralReferenceAttribute() {
        final Term original = Generator.generateTermWithId();
        final Term changed = cloneOf(original);
        original.setParentTerms(Collections.singleton(Generator.generateTermWithId()));
        changed.setParentTerms(Collections.singleton(Generator.generateTermWithId()));

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        assertEquals(original.getUri().toString(), record.getObjectId());
        assertEquals(SKOS.BROADER, record.getAttributeName());
    }

    @Test
    void calculateChangesDiscoversChangesInSingularIdentifierBasedReferenceAttribute() {
        final Term original = Generator.generateTermWithId();
        original.setGlossary(Generator.generateUri());
        final Term changed = cloneOf(original);
        changed.setGlossary(Generator.generateUri());
        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        assertEquals(original.getUri().toString(), record.getObjectId());
        assertEquals(SKOS.IN_SCHEME, record.getAttributeName());
    }

    @Test
    void calculateChangesDiscoversChangesInPluralIdentifierBasedReferenceAttribute() {
        final Vocabulary original = Generator.generateVocabularyWithId();
        final Vocabulary changed = cloneOf(original);
        original.setImportedVocabularies(
                IntStream.range(0, 5).mapToObj(i -> Generator.generateUri()).collect(Collectors.toSet()));
        changed.setImportedVocabularies(new HashSet<>(original.getImportedVocabularies()));
        changed.getImportedVocabularies().add(Generator.generateUri());

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        assertEquals(original.getUri().toString(), record.getObjectId());
        assertEquals(cz.cvut.kbss.termit.util.Vocabulary.s_p_importuje_slovnik,
                record.getAttributeName());
    }

    @Test
    void calculateChangesSkipsInferredAttributes() {
        final Document original = Generator.generateDocumentWithId();
        final Document changed = new Document();
        changed.setUri(original.getUri());
        changed.setLabel(original.getLabel());
        changed.setDescription(original.getDescription());
        original.setVocabulary(Generator.generateUri());

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertTrue(result.isEmpty());
    }

    @Test
    void calculateChangesReturnsEmptyCollectionForIdenticalOriginalAndUpdate() {
        final Term original = Generator.generateTermWithId();
        final Term changed = cloneOf(original);

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertTrue(result.isEmpty());
    }

    @Test
    void calculateChangesHandlesChangeToNullInReference() {
        final Term original = Generator.generateTermWithId();
        final Term changed = cloneOf(original);
        original.setParentTerms(Collections.singleton(Generator.generateTermWithId()));

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        assertEquals(original.getUri().toString(), record.getObjectId());
        assertEquals(SKOS.BROADER, record.getAttributeName());
    }

    @Test
    void calculateChangesDiscoversChangeInTypes() {
        final Term original = Generator.generateTermWithId();
        final Term changed = cloneOf(original);
        original.setTypes(Collections.singleton(Generator.generateUri().toString()));

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        assertEquals(original.getUri().toString(), record.getObjectId());
        assertEquals(RDF.TYPE, record.getAttributeName());
    }

    @Test
    void calculateChangesDiscoversChangeInUnmappedProperties() {
        final Term original = Generator.generateTermWithId();
        final Term changed = cloneOf(original);
        final URI property = Generator.generateUri();
        original.setProperties(Collections.singletonMap(property.toString(), Collections.singleton("Test")));

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        assertEquals(original.getUri().toString(), record.getObjectId());
        assertEquals(property.toString(), record.getAttributeName());
    }

    @Test
    void calculateChangesDiscoversChangeInUpdatedUnmappedProperties() {
        final Term original = Generator.generateTermWithId();
        final Term changed = cloneOf(original);
        final URI property = Generator.generateUri();
        changed.setProperties(Collections.singletonMap(property.toString(), Collections.singleton("Test")));

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        assertEquals(original.getUri().toString(), record.getObjectId());
        assertEquals(property.toString(), record.getAttributeName());
    }

    @Test
    void calculateChangesDiscoversChangeInValuesOfSingleUnmappedProperty() {
        final Term original = Generator.generateTermWithId();
        final Term changed = cloneOf(original);
        final URI property = Generator.generateUri();
        original.setProperties(Collections.singletonMap(property.toString(), Collections.singleton("Test")));
        changed.setProperties(Collections.singletonMap(property.toString(), Collections.singleton("Different test")));

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        assertEquals(original.getUri().toString(), record.getObjectId());
        assertEquals(property.toString(), record.getAttributeName());
    }

    @Test
    void calculateChangesDiscoversMultipleChangesAtOnce() {
        final Term original = Generator.generateTermWithId();
        final Term changed = cloneOf(original);
        original.setParentTerms(Collections.singleton(Generator.generateTermWithId()));
        changed.setTypes(Collections.singleton(Generator.generateUri().toString()));
        changed.getLabel().set(Environment.LANGUAGE, "Updated label");

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(r -> r.getAttributeName().equals(RDF.TYPE)));
        assertTrue(result.stream().anyMatch(r -> r.getAttributeName().equals(SKOS.BROADER)));
        assertTrue(result.stream().anyMatch(r -> r.getAttributeName().equals(SKOS.PREF_LABEL)));
    }

    @Test
    void calculateChangesReturnsChangeRecordWithOriginalAndNewValueOfSingularLiteralAttribute() {
        final Vocabulary original = Generator.generateVocabularyWithId();
        original.getModel().setUri(Generator.generateUri());
        original.getGlossary().setUri(Generator.generateUri());
        final Vocabulary changed = cloneOf(original);
        changed.setLabel("Updated label");

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        // FIXME: why collection?
        assertEquals(original.getLabel(), record.getPreviousValue());
        //assertEquals(Collections.singleton(changed.getLabel()), record.getNewValue());
    }

    @Test
    void calculateChangesReturnsChangeRecordWithOriginalAndNewValueOfSingularReferenceAttribute() {
        // Note: This does not normally happen, it simulates possible changes in model when assets would have singular references to other objects
        final Vocabulary original = Generator.generateVocabularyWithId();
        original.getModel().setUri(Generator.generateUri());
        original.getGlossary().setUri(Generator.generateUri());
        final Vocabulary changed = cloneOf(original);
        changed.setGlossary(new Glossary());
        changed.getGlossary().setUri(Generator.generateUri());

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        // FIXME: why collection?
        assertEquals(original.getGlossary().getUri(), record.getPreviousValue());
        //assertEquals(Collections.singleton(changed.getGlossary().getUri()), record.getNewValue());
    }

    @Test
    void calculateChangesReturnsChangeRecordWithOriginalAndNewValueOfPluralLiteralAttribute() {
        final Term original = Generator.generateTermWithId();
        original.setSources(null);
        final Term changed = cloneOf(original);
        changed.setSources(IntStream.range(0, 5).mapToObj(i -> "http://source" + i).collect(Collectors.toSet()));

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        assertNull(record.getPreviousValue());
        //assertEquals(changed.getSources(), record.getNewValue());
    }

    @Test
    void calculateChangesReturnsChangeRecordWithOriginalAndNewValueOfPluralReferenceAttribute() {
        final Term original = Generator.generateTermWithId();
        final Term changed = cloneOf(original);
        original.setParentTerms(Collections.singleton(Generator.generateTermWithId()));
        changed.setParentTerms(Collections.singleton(Generator.generateTermWithId()));

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        assertEquals(original.getParentTerms().stream().map(Term::getUri).collect(Collectors.toSet()),
                record.getPreviousValue());
       /* assertEquals(changed.getParentTerms().stream().map(Term::getUri).collect(Collectors.toSet()),
                record.getNewValue());*/
    }

    @Test
    void calculateChangesReturnsChangeRecordWithOriginalAndNewValueOfSingularIdentifierBasedReferenceAttribute() {
        final Term original = Generator.generateTermWithId();
        original.setGlossary(Generator.generateUri());
        final Term changed = cloneOf(original);
        changed.setGlossary(Generator.generateUri());

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        assertEquals(Collections.singleton(original.getGlossary()), record.getPreviousValue());
        //assertEquals(Collections.singleton(changed.getGlossary()), record.getNewValue());
    }

    @Test
    void calculateChangesReturnsChangeRecordWithOriginalAndNewValueOfPluralIdentifierBasedReferenceAttribute() {
        final Vocabulary original = Generator.generateVocabularyWithId();
        final Vocabulary changed = cloneOf(original);
        original.setImportedVocabularies(
                IntStream.range(0, 5).mapToObj(i -> Generator.generateUri()).collect(Collectors.toSet()));
        changed.setImportedVocabularies(new HashSet<>(original.getImportedVocabularies()));
        changed.getImportedVocabularies().add(Generator.generateUri());

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        assertEquals(original.getImportedVocabularies(), record.getPreviousValue());
        //assertEquals(changed.getImportedVocabularies(), record.getNewValue());
    }

    @Test
    void calculateChangesReturnsChangeRecordWithOriginalAndNewValueOfTypes() {
        final Term original = Generator.generateTermWithId();
        final Term changed = cloneOf(original);
        original.setTypes(Collections.singleton(Generator.generateUri().toString()));

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        assertEquals(original.getTypes().stream().map(URI::create).collect(Collectors.toSet()),
                record.getPreviousValue());
        //assertNull(record.getNewValue());
    }

    @Test
    void calculateChangesReturnsChangeRecordWithOriginalAndNewValueOfUnmappedProperty() {
        final Term original = Generator.generateTermWithId();
        final Term changed = cloneOf(original);
        final URI property = Generator.generateUri();
        original.setProperties(Collections.singletonMap(property.toString(), Collections.singleton("Test")));
        changed.setProperties(Collections.singletonMap(property.toString(), Collections.singleton("Different test")));

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        assertEquals(original.getProperties().get(property.toString()), record.getPreviousValue());
        //assertEquals(changed.getProperties().get(property.toString()), record.getNewValue());
    }

    @Test
    void calculateChangesReturnsChangeRecordWithEmptyOriginalAndAddedNewValueOfUnmappedProperty() {
        final Term original = Generator.generateTermWithId();
        final Term changed = cloneOf(original);
        final URI property = Generator.generateUri();
        changed.setProperties(Collections.singletonMap(property.toString(), Collections.singleton("Different test")));

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        assertNull(record.getPreviousValue());
        //assertEquals(changed.getProperties().get(property.toString()), record.getNewValue());
    }

    @Test
    void calculateChangesReturnsChangeRecordWithEmptyOriginalAndAddedNewValueOfReferenceAttribute() {
        final Term original = Generator.generateTermWithId();
        final Term changed = cloneOf(original);
        changed.setParentTerms(Collections.singleton(Generator.generateTermWithId()));

        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        // TODO
        //assertThat(record.getPreviousValue(), anyOf(nullValue(), emptyCollectionOf(Object.class)));
        /*assertEquals(changed.getParentTerms().stream().map(Term::getUri).collect(Collectors.toSet()),
                record.getNewValue());*/
    }

    @Test
    void calculateChangesDoesNotRegisterChangeInTypesWhenOriginalIsEmptyAndUpdateIsNull() {
        final Term original = Generator.generateTermWithId();
        original.setTypes(Collections.emptySet());
        final Term changed = cloneOf(original);
        changed.setTypes(null);
        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        // TODO: implement in changetracking?
        assertTrue(result.isEmpty());
    }

    @Test
    void calculateChangesRegistersAddedTranslationInMultilingualTermLabel() {
        final Term original = Generator.generateTermWithId();
        final Term changed = cloneOf(original);
        changed.getLabel().set("cs", "Testovací pojem");
        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        assertEquals(1, result.size());
        final ChangeVector<?> record = result.iterator().next();
        assertEquals(original.getLabel(), record.getPreviousValue());
    }

    @Test
    void calculateChangesDoesNotRegisterChangeInPluralAssociationWhenOriginalIsNullAndUpdateIsEmpty() {
        final Term original = Generator.generateTermWithId();
        final Term changed = cloneOf(original);
        changed.setExternalParentTerms(new HashSet<>());
        final Collection<ChangeVector<?>> result = sut.getChangeVectors(original, changed, true);
        // TODO: implement in changetracking?
        assertTrue(result.isEmpty());
        //assertThat(result, emptyCollectionOf(ChangeVector.class));
    }
}
