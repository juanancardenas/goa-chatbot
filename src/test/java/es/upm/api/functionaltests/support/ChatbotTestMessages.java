package es.upm.api.functionaltests.support;

public final class ChatbotTestMessages {
    private ChatbotTestMessages() {
    }

    public static final String CLIENT_GENERAL_START_REPLY =
            "Hola. Soy tu asistente virtual y puedo ayudarte con dudas sobre tu encargo, su estado o los próximos pasos.";
    public static final String PROFESSIONAL_GENERAL_START_REPLY =
            "Conversación iniciada correctamente. Puedes consultar dudas operativas, funcionales o de gestión relacionadas con el encargo y la plataforma.";

    public static final String CLIENT_MESSAGE_REPLY =
            "He recibido tu mensaje. De momento estoy en una versión inicial, pero intentaré ayudarte de forma clara con los siguientes pasos o con el estado de tu consulta.";
    public static final String PROFESSIONAL_MESSAGE_REPLY =
            "Mensaje recibido. La integración actual sigue siendo simulada, pero la respuesta se orienta a soporte operativo y gestión funcional del encargo.";

    public static final String MISSING_CASE_CONTEXT_REPLY =
            "Esta conversación es general y no está asociada a un encargo concreto. Para responder sobre el estado, documentos o pasos de un caso, abre el asistente desde la hoja de encargo correspondiente.";

    public static final String OUT_OF_CASE_SCOPE_REPLY =
            "Solo puedo responder dentro del ámbito del encargo activo. Si necesitas consultar otro caso, abre una conversación desde la hoja de encargo correspondiente.";

    public static final String LEGAL_BINDING_ADVICE_REPLY =
            "No puedo emitir asesoramiento legal vinculante ni indicar una estrategia jurídica definitiva. Puedo ofrecer orientación general y ayudarte a revisar la información disponible en la plataforma.";
}
