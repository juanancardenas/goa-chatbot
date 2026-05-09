package es.upm.api.functionaltests.support;

import es.upm.api.domain.common.ChatbotResponseMessages;

public final class ChatbotTestMessages {
    private ChatbotTestMessages() {
    }

    public static final String CLIENT_GENERAL_START_REPLY =
            ChatbotResponseMessages.CLIENT_GENERAL_START_REPLY;
    public static final String PROFESSIONAL_GENERAL_START_REPLY =
            ChatbotResponseMessages.PROFESSIONAL_GENERAL_START_REPLY;

    public static final String CLIENT_MESSAGE_REPLY =
            "He recibido tu mensaje. De momento estoy en una versión inicial, pero intentaré ayudarte de forma clara con los siguientes pasos o con el estado de tu consulta.";
    public static final String PROFESSIONAL_MESSAGE_REPLY =
            "Mensaje recibido. La integración actual sigue siendo simulada, pero la respuesta se orienta a soporte operativo y gestión funcional del encargo.";

    public static final String CLIENT_GENERAL_STATUS_REPLY =
            ChatbotResponseMessages.CLIENT_GENERAL_STATUS_REPLY;

    public static final String PROFESSIONAL_GENERAL_STATUS_REPLY =
            ChatbotResponseMessages.PROFESSIONAL_GENERAL_STATUS_REPLY;

    public static final String CLIENT_GENERAL_TIMELINE_REPLY =
            ChatbotResponseMessages.CLIENT_GENERAL_TIMELINE_REPLY;

    public static final String PROFESSIONAL_GENERAL_TIMELINE_REPLY =
            ChatbotResponseMessages.PROFESSIONAL_GENERAL_TIMELINE_REPLY;

    public static final String CLIENT_GENERAL_DOCUMENTS_REPLY =
            ChatbotResponseMessages.CLIENT_GENERAL_DOCUMENTS_REPLY;

    public static final String PROFESSIONAL_GENERAL_DOCUMENTS_REPLY =
            ChatbotResponseMessages.PROFESSIONAL_GENERAL_DOCUMENTS_REPLY;

    public static final String CLIENT_GENERAL_CONTEXT_REPLY =
            ChatbotResponseMessages.CLIENT_GENERAL_CONTEXT_REPLY;

    public static final String PROFESSIONAL_GENERAL_CONTEXT_REPLY =
            ChatbotResponseMessages.PROFESSIONAL_GENERAL_CONTEXT_REPLY;

    public static final String MISSING_CASE_CONTEXT_REPLY =
            ChatbotResponseMessages.MISSING_CASE_CONTEXT_REPLY;

    public static final String OUT_OF_CASE_SCOPE_REPLY =
            ChatbotResponseMessages.OUT_OF_CASE_SCOPE_REPLY;

    public static final String LEGAL_BINDING_ADVICE_REPLY =
            ChatbotResponseMessages.LEGAL_BINDING_ADVICE_REPLY;
}
