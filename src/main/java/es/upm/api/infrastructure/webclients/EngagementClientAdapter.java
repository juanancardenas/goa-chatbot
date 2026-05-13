package es.upm.api.infrastructure.webclients;

import es.upm.api.domain.model.platform.EngagementEventPage;
import es.upm.api.domain.model.platform.EngagementLetterSummary;
import es.upm.api.domain.ports.out.EngagementClient;
import org.springframework.stereotype.Component;

@Component
public class EngagementClientAdapter implements EngagementClient {

    private final EngagementFeignClient engagementFeignClient;

    public EngagementClientAdapter(EngagementFeignClient engagementFeignClient) {
        this.engagementFeignClient = engagementFeignClient;
    }

    @Override
    public EngagementLetterSummary readById(String id) {
        return this.engagementFeignClient.readById(id);
    }

    @Override
    public EngagementEventPage readEventsByEngagementLetterId(String id, int page, int size) {
        return this.engagementFeignClient.readEventsByEngagementLetterId(id, page, size);
    }
}
