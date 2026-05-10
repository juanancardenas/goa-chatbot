package es.upm.api.domain.ports.out;

import es.upm.api.domain.model.platform.EngagementEventPage;
import es.upm.api.domain.model.platform.EngagementLetterSummary;

public interface EngagementClient {

    EngagementLetterSummary readById(String id);

    EngagementEventPage readEventsByEngagementLetterId(String id, int page, int size);
}