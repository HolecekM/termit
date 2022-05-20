package cz.cvut.kbss.termit.util.json;

import cz.cvut.kbss.changetracking.model.ChangeVector;
import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.jsonld.serialization.CommonValueSerializers;
import cz.cvut.kbss.jsonld.serialization.JsonNodeFactory;
import cz.cvut.kbss.jsonld.serialization.ValueSerializer;
import cz.cvut.kbss.jsonld.serialization.model.JsonNode;
import cz.cvut.kbss.jsonld.serialization.model.ObjectNode;
import cz.cvut.kbss.jsonld.serialization.traversal.SerializationContext;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.util.Set;

/**
 * JSON-LD serializer for the {@link ChangeVector} class from the change-tracking module.
 */
@SuppressWarnings("rawtypes")
public class ChangeVectorSerializer implements ValueSerializer<ChangeVector> {
    protected final CommonValueSerializers serializers;

    public ChangeVectorSerializer() {
        serializers = new CommonValueSerializers();
    }

    @Override
    public JsonNode serialize(
            ChangeVector vector,
            SerializationContext<ChangeVector> ctx
    ) {
        ObjectNode root = JsonNodeFactory.createObjectNode();
        root.addItem(serializeRaw(JsonLd.TYPE, Set.of(Vocabulary.s_c_zmena, Vocabulary.s_c_uprava_entity)));
        root.addItem(serializeRaw(Vocabulary.s_p_ma_datum_a_cas_modifikace, vector.getTimestamp()));
        root.addItem(serializeAnnotationProperty(Vocabulary.s_p_ma_editora, vector.getAuthorId()));
        root.addItem(serializeAnnotationProperty(Vocabulary.s_p_ma_zmenenou_entitu, vector.getObjectId()));
        root.addItem(serializeAnnotationProperty(Vocabulary.s_p_ma_zmeneny_atribut, vector.getAttributeName()));

        if (vector.getPreviousValue() != null) {
            root.addItem(serializeRaw(Vocabulary.s_p_ma_puvodni_hodnotu, vector.getPreviousValue()));
        }

        return root;
    }

    private <T> JsonNode serializeRaw(String attributeName, T value) {
        SerializationContext<Object> ctx = new SerializationContext<>(attributeName, value);
        return serializers.getOrDefault(ctx).serialize(value, ctx);
    }

    private JsonNode serializeAnnotationProperty(String attributeName, String value) {
        ObjectNode node = JsonNodeFactory.createObjectNode(attributeName);
        node.addItem(JsonNodeFactory.createObjectIdNode(JsonLd.ID, value));
        return node;
    }
}
