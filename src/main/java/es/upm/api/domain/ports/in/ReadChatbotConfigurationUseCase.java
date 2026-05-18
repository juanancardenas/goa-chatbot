package es.upm.api.domain.ports.in;

import es.upm.api.domain.model.chatbot.result.ChatbotConfigurationResult;

public interface ReadChatbotConfigurationUseCase {

    ChatbotConfigurationResult readConfigurationStatus();
}
