package SIGA.backend.dtos;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class IntegerRequestDeserializer extends ValueDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) {
        if (parser.hasToken(JsonToken.VALUE_NUMBER_INT)) return parser.getIntValue();
        if (parser.hasToken(JsonToken.VALUE_STRING) && parser.getString().matches("-?[0-9]+")) {
            try {
                return Integer.valueOf(parser.getString());
            } catch (NumberFormatException exception) {
                return context.reportInputMismatch(Integer.class, "El número entero supera el rango permitido");
            }
        }
        return context.reportInputMismatch(Integer.class, "Ingrese un número entero válido, sin decimales");
    }
}
