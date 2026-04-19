package es.upm.api.domain.services.support;

public final class ChatbotResponseMessages {
    private ChatbotResponseMessages() {
    }

    public static final String CLIENT_GENERAL_START_REPLY =
            "Hola. Soy tu asistente virtual y puedo ayudarte con dudas sobre tu encargo, su estado o los próximos pasos.";
    public static final String PROFESSIONAL_GENERAL_START_REPLY =
            "Conversación iniciada correctamente. Puedes consultar dudas operativas, funcionales o de gestión relacionadas con el encargo y la plataforma.";

    public static final String CLIENT_MESSAGE_REPLY =
            "He recibido tu mensaje. De momento estoy en una versión inicial, pero intentaré ayudarte de forma clara con los siguientes pasos o con el estado de tu consulta.";
    public static final String PROFESSIONAL_MESSAGE_REPLY =
            "Mensaje recibido. La integración actual sigue siendo simulada, pero la respuesta se orienta a soporte operativo y gestión funcional del encargo.";
}
