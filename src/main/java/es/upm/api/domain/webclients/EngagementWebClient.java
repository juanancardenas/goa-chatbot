package es.upm.api.domain.webclients;

import es.upm.api.configurations.FeignConfig;
import es.upm.api.domain.model.platform.EngagementEventPage;
import es.upm.api.domain.model.platform.EngagementLetterSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "goa-engagement",
        url = "${goa.engagement.base-url}",
        configuration = FeignConfig.class
)
public interface EngagementWebClient {
    String ENGAGEMENT_LETTERS = "/engagement-letters";
    String ID_ID = "/{id}";
    String EVENTS = "/events";

    @GetMapping(ENGAGEMENT_LETTERS + ID_ID)
    EngagementLetterSummary readById(@PathVariable String id);

    @GetMapping(ENGAGEMENT_LETTERS + ID_ID + EVENTS)
    EngagementEventPage readEventsByEngagementLetterId(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    );
}
