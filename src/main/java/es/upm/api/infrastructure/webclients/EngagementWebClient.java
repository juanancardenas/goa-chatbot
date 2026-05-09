package es.upm.api.infrastructure.webclients;

import es.upm.api.configurations.FeignConfig;
import es.upm.api.domain.model.platform.EngagementEventPage;
import es.upm.api.domain.model.platform.EngagementLetterSummary;
import es.upm.api.domain.ports.out.EngagementClientFinder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "goa-engagement",
        url = "${goa.engagement.base-url}",
        configuration = FeignConfig.class
)
public interface EngagementWebClient extends EngagementClientFinder {

    String ENGAGEMENT_LETTERS = "/engagement-letters";
    String ID_ID = "/{id}";
    String EVENTS = "/events";

    @Override
    @GetMapping(ENGAGEMENT_LETTERS + ID_ID)
    EngagementLetterSummary readById(@PathVariable String id);

    @Override
    @GetMapping(ENGAGEMENT_LETTERS + ID_ID + EVENTS)
    EngagementEventPage readEventsByEngagementLetterId(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    );
}
