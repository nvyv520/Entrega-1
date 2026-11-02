package SIGA.backend.dtos;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class LongRequestDeserializer extends ValueDeserializer<Long> {

    @Override
    public Long deserialize(JsonParser parser, DeserializationContext context) {
        if (parser.hasToken(JsonToken.VALUE_NUMBER_INT)) return parser.getLongValue();
        if (parser.hasToken(JsonToken.VALUE_STRING) && parser.getString().matches("-?[0-9]+")) {
            try {
                return Long.valueOf(parser.getString());
            } catch (NumberFormatException exception) {
                return context.reportInputMismatch(Long.class, "El identificador supera el rango permitido");
            }
        }
        return context.reportInputMismatch(Long.class, "Ingrese un identificador entero válido, sin decimales");
    }
}
