package es.upm.api.domain.services.classification;

import es.upm.api.domain.enums.PlatformQuestionType;

import java.util.Optional;

public final class ChatbotQuestionTypes {

    private ChatbotQuestionTypes() {
    }

    public static PlatformQuestionType classifyOrGeneralContext(
            ChatbotQuestionClassifier chatbotQuestionClassifier,
            String userMessage
    ) {
        return Optional.ofNullable(chatbotQuestionClassifier.classify(userMessage))
                .orElse(PlatformQuestionType.GENERAL_CONTEXT);
    }
}
