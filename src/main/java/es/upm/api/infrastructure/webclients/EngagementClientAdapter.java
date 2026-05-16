package es.upm.api.infrastructure.webclients;

import es.upm.api.domain.model.platform.EngagementEventPage;
import es.upm.api.domain.model.platform.EngagementLetterSummary;
import es.upm.api.domain.ports.out.EngagementClient;
import es.upm.api.infrastructure.webclients.engagement.EngagementFeignMapper;
import org.springframework.stereotype.Component;

@Component
public class EngagementClientAdapter implements EngagementClient {

    private final EngagementFeignClient engagementFeignClient;
    private final EngagementFeignMapper engagementFeignMapper;

    public EngagementClientAdapter(
            EngagementFeignClient engagementFeignClient,
            EngagementFeignMapper engagementFeignMapper
    ) {
        this.engagementFeignClient = engagementFeignClient;
        this.engagementFeignMapper = engagementFeignMapper;
    }

    @Override
    public EngagementLetterSummary readById(String id) {
        return this.engagementFeignMapper.toDomain(this.engagementFeignClient.readById(id));
    }

    @Override
    public EngagementEventPage readEventsByEngagementLetterId(String id, int page, int size) {
        return this.engagementFeignMapper.toDomain(
                this.engagementFeignClient.readEventsByEngagementLetterId(id, page, size)
        );
    }
}
