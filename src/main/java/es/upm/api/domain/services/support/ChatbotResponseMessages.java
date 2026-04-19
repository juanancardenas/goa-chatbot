package es.upm.api.domain.services.support;

public final class ChatbotResponseMessages {
    private ChatbotResponseMessages() {
    }

    //ChatbotService

    public static final String CLIENT_GENERAL_START_REPLY =
            "Hola. Soy tu asistente virtual y puedo ayudarte con dudas sobre tu encargo, su estado o los próximos pasos.";
    public static final String PROFESSIONAL_GENERAL_START_REPLY =
            "Conversación iniciada correctamente. Puedes consultar dudas operativas, funcionales o de gestión relacionadas con el encargo y la plataforma.";

    public static final String CLIENT_MESSAGE_REPLY =
            "He recibido tu mensaje. De momento estoy en una versión inicial, pero intentaré ayudarte de forma clara con los siguientes pasos o con el estado de tu consulta.";
    public static final String PROFESSIONAL_MESSAGE_REPLY =
            "Mensaje recibido. La integración actual sigue siendo simulada, pero la respuesta se orienta a soporte operativo y gestión funcional del encargo.";

    public static final String CONTEXTUAL_PLATFORM_DATA_REPLY_TEMPLATE =
            "He revisado la hoja de encargo %s. El encargo está asociado a %s y puedo apoyarme en información interna disponible del caso para responder con más precisión.";

    public static final String CONTEXTUAL_PLATFORM_DATA_PROCEDURES_TEMPLATE =
            "Los procedimientos visibles en este encargo incluyen: %s.";

    public static final String CONTEXTUAL_PLATFORM_DATA_UNAVAILABLE_REPLY =
            "No he podido recuperar en este momento el contexto actualizado del encargo activo. Puedo seguir ayudándote, pero ahora mismo no debo responder como si tuviera datos internos confirmados del caso.";


    //ScopePolicy

    public static final String MISSING_CASE_CONTEXT_REPLY =
            "Esta conversación es general y no está asociada a un encargo concreto. Para responder sobre el estado, documentos o pasos de un caso, abre el asistente desde la hoja de encargo correspondiente.";

    public static final String OUT_OF_CASE_SCOPE_REPLY =
            "Solo puedo responder dentro del ámbito del encargo activo. Si necesitas consultar otro caso, abre una conversación desde la hoja de encargo correspondiente.";

    public static final String LEGAL_BINDING_ADVICE_REPLY =
            "No puedo emitir asesoramiento legal vinculante ni indicar una estrategia jurídica definitiva. Puedo ofrecer orientación general y ayudarte a revisar la información disponible en la plataforma.";

    public static final String UNSUPPORTED_FACTUAL_ASSERTION_REPLY =
            "No debo afirmar hechos que no estén disponibles en el contexto actual. Puedo ayudarte con orientación general o con la información visible del encargo activo.";

    public static final String AMBIGUOUS_CONTEXT_REPLY =
            "Tu consulta necesita más contexto para responder con seguridad. Si se refiere a un encargo concreto, abre el asistente desde esa hoja de encargo.";
}
