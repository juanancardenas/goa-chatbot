package es.upm.api.domain.services;

import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import es.upm.api.domain.model.platform.EngagementEventPage;
import es.upm.api.domain.model.platform.EngagementEventSummary;
import es.upm.api.domain.model.platform.EngagementLetterSummary;
import es.upm.api.domain.model.platform.LegalProcedureSummary;
import es.upm.api.domain.webclients.EngagementWebClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ChatbotPlatformContextService {
    private final EngagementWebClient engagementWebClient;

    public ChatbotPlatformContextService(EngagementWebClient engagementWebClient) {
        this.engagementWebClient = engagementWebClient;
    }

    public Optional<ChatbotPlatformContext> loadContext(String engagementLetterId) {
        if (engagementLetterId == null || engagementLetterId.isBlank()) {
            return Optional.empty();
        }

        try {
            EngagementLetterSummary engagementLetter = this.engagementWebClient.readById(engagementLetterId);
            EngagementEventPage eventsPage = this.readEventsSafely(engagementLetterId);

            List<String> procedureTitles = Optional.ofNullable(engagementLetter.getLegalProcedures())
                    .orElse(List.of())
                    .stream()
                    .map(LegalProcedureSummary::getTitle)
                    .filter(title -> title != null && !title.isBlank())
                    .toList();

            List<String> recentEventSummaries = Optional.ofNullable(eventsPage)
                    .map(EngagementEventPage::getContent)
                    .orElse(List.of())
                    .stream()
                    .map(EngagementEventSummary::displayText)
                    .limit(3)
                    .toList();

            List<String> sourcesSummary = new ArrayList<>();
            sourcesSummary.add("Hoja de encargo");
            procedureTitles.stream()
                    .limit(2)
                    .map(title -> "Procedimiento: " + title)
                    .forEach(sourcesSummary::add);
            recentEventSummaries.stream()
                    .limit(2)
                    .map(event -> "Hito/evento: " + event)
                    .forEach(sourcesSummary::add);

            return Optional.of(
                    ChatbotPlatformContext.builder()
                            .engagementLetterId(engagementLetterId)
                            .ownerDisplayName(
                                    engagementLetter.getOwner() == null
                                            ? "usuario del encargo"
                                            : engagementLetter.getOwner().displayName()
                            )
                            .procedureTitles(procedureTitles)
                            .recentEventSummaries(recentEventSummaries)
                            .sourcesSummary(sourcesSummary)
                            .build()
            );
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private EngagementEventPage readEventsSafely(String engagementLetterId) {
        try {
            return this.engagementWebClient.readEventsByEngagementLetterId(engagementLetterId, 0, 5);
        } catch (RuntimeException ignored) {
            return new EngagementEventPage(List.of());
        }
    }
}
