package es.upm.api.domain.services;

import es.upm.api.domain.model.platform.ChatbotPlatformContext;
import es.upm.api.domain.model.platform.EngagementEventPage;
import es.upm.api.domain.model.platform.EngagementEventSummary;
import es.upm.api.domain.model.platform.EngagementLetterSummary;
import es.upm.api.domain.model.platform.LegalProcedureSummary;
import es.upm.api.domain.model.platform.UserSummary;
import es.upm.api.domain.ports.out.EngagementClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChatbotPlatformContextService {
    private static final String DEFAULT_OWNER = "usuario del encargo";

    private final EngagementClient engagementClient;

    public ChatbotPlatformContextService(EngagementClient engagementClient) {
        this.engagementClient = engagementClient;
    }

    public Optional<ChatbotPlatformContext> loadContext(String engagementLetterId) {
        if (engagementLetterId == null || engagementLetterId.isBlank()) {
            log.warn("Platform context not loaded because engagementLetterId is blank");
            return Optional.empty();
        }

        try {
            log.info("Loading platform context for engagementLetterId={}", engagementLetterId);

            Optional<EngagementLetterSummary> engagementLetter = this.readEngagementLetterSafely(engagementLetterId);
            List<String> recentEventSummaries = this.readRecentEventSummaries(engagementLetterId);

            log.info(
                    "Engagement context response. engagementLetterId={}, found={}",
                    engagementLetterId,
                    engagementLetter.isPresent()
            );

            String ownerDisplayName = engagementLetter
                    .map(EngagementLetterSummary::getOwner)
                    .map(UserSummary::displayName)
                    .filter(name -> name != null && !name.isBlank())
                    .orElse(DEFAULT_OWNER);

            List<String> procedureTitles = engagementLetter
                    .map(EngagementLetterSummary::getLegalProcedures)
                    .orElse(List.of())
                    .stream()
                    .map(LegalProcedureSummary::getTitle)
                    .filter(title -> title != null && !title.isBlank())
                    .distinct()
                    .toList();

            List<String> legalTaskSummaries = this.buildLegalTaskSummaries(engagementLetter);

            if (procedureTitles.isEmpty() && legalTaskSummaries.isEmpty() && recentEventSummaries.isEmpty() && engagementLetter.isEmpty()) {
                return Optional.empty();
            }

            List<String> sourcesSummary = new ArrayList<>();
            sourcesSummary.add("Hoja de encargo " + engagementLetterId);

            procedureTitles.stream()
                    .limit(2)
                    .map(title -> "Procedimiento: " + title)
                    .forEach(sourcesSummary::add);

            legalTaskSummaries.stream()
                    .limit(3)
                    .map(task -> "Legal Task: " + task)
                    .forEach(sourcesSummary::add);

            recentEventSummaries.stream()
                    .limit(2)
                    .map(event -> "Hito/evento: " + event)
                    .forEach(sourcesSummary::add);

            log.info(
                    "Platform context built. engagementLetterId={}, procedures={}, legalTasks={}, events={}, sources={}",
                    engagementLetterId,
                    procedureTitles.size(),
                    legalTaskSummaries.size(),
                    recentEventSummaries.size(),
                    sourcesSummary.size()
            );

            return Optional.of(
                    ChatbotPlatformContext.builder()
                            .engagementLetterId(engagementLetterId)
                            .ownerDisplayName(ownerDisplayName)
                            .procedureTitles(procedureTitles)
                            .legalTaskSummaries(legalTaskSummaries)
                            .recentEventSummaries(recentEventSummaries)
                            .sourcesSummary(sourcesSummary)
                            .build()
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "Could not load platform context. engagementLetterId={}, error={}, message={}",
                    engagementLetterId,
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
            return Optional.empty();
        }
    }

    private Optional<EngagementLetterSummary> readEngagementLetterSafely(String engagementLetterId) {
        try {
            return Optional.ofNullable(this.engagementClient.readById(engagementLetterId));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private List<String> readRecentEventSummaries(String engagementLetterId) {
        try {
            EngagementEventPage eventsPage = this.engagementClient.readEventsByEngagementLetterId(engagementLetterId, 0, 5);

            return Optional.ofNullable(eventsPage)
                    .map(EngagementEventPage::getContent)
                    .orElse(List.of())
                    .stream()
                    .map(EngagementEventSummary::displayText)
                    .filter(text -> text != null && !text.isBlank())
                    .limit(3)
                    .toList();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private List<String> buildLegalTaskSummaries(Optional<EngagementLetterSummary> engagementLetter) {
        return engagementLetter
                .map(EngagementLetterSummary::getLegalProcedures)
                .orElse(List.of())
                .stream()
                .filter(procedure -> procedure.getLegalTasks() != null)
                .flatMap(procedure -> procedure.getLegalTasks().stream()
                        .filter(task -> task != null && !task.isBlank())
                        .map(task -> "%s: %s".formatted(
                                this.safeText(procedure.getTitle(), "Procedimiento sin título"),
                                task.trim()
                        ))
                )
                .distinct()
                .toList();
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }

}
