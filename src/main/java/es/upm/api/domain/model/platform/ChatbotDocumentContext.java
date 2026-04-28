package es.upm.api.domain.model.platform;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class ChatbotDocumentContext {
    private boolean available;
    private boolean authorizedSourceConfigured;
    private List<String> visibleDocumentTitles;
    private List<String> sourcesSummary;
}
