package cz.cvut.kbss.termit.service.changetracking;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import cz.cvut.kbss.jopa.adapters.IndirectMultilingualString;
import cz.cvut.kbss.jopa.model.MultilingualString;

import java.io.IOException;
import java.util.HashMap;

// TODO: rename - actually deserializes MultilingualString, not Indirect-
public class IMSDeserializer extends StdDeserializer<MultilingualString> {
    public IMSDeserializer() {
        this(IndirectMultilingualString.class);
    }

    protected IMSDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public MultilingualString deserialize(
            JsonParser jsonParser,
            DeserializationContext deserializationContext
    ) throws IOException {
        var tree = jsonParser.getCodec().readTree(jsonParser);
        var languageIterator = ((ArrayNode) tree.get("languages")).elements();
        var map = new HashMap<String, String>();
        var values = tree.get("value");
        while (languageIterator.hasNext()) {
            var lang = languageIterator.next().textValue();
            map.put(lang, ((JsonNode) values.get(lang)).textValue());
        }
        return new MultilingualString(map);
    }
}
