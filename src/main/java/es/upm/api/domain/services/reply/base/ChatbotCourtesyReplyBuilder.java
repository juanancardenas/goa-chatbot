package es.upm.api.domain.services.reply.base;

import es.upm.api.domain.common.ChatbotResponseMessages;
import es.upm.api.domain.enums.ConversationProfileType;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class ChatbotCourtesyReplyBuilder {

    public boolean isCourtesyMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        String normalized = message.toLowerCase(Locale.ROOT);

        return normalized.contains("gracias")
                || normalized.contains("por favor")
                || normalized.contains("te quiero")
                || normalized.contains("te amo")
                || normalized.contains("buen dia")
                || normalized.contains("buen día")
                || normalized.contains("buenas")
                || normalized.contains("hola")
                || normalized.contains("hasta luego")
                || normalized.contains("nos vemos");
    }

    public String courtesyReply(ConversationProfileType profile) {
        return switch (profile) {
            case CLIENT -> ChatbotResponseMessages.CLIENT_COURTESY_REPLY;
            case PROFESSIONAL -> ChatbotResponseMessages.PROFESSIONAL_COURTESY_REPLY;
        };
    }
}
