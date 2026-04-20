package es.upm.api.domain.model.platform;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class ChatbotPlatformContext {
    private String engagementLetterId;
    private String ownerDisplayName;
    private List<String> procedureTitles;
    private List<String> recentEventSummaries;
    private List<String> sourcesSummary;
}
