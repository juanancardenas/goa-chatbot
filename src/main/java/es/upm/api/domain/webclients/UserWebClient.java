package es.upm.api.domain.webclients;

import es.upm.api.configurations.FeignConfig;
import es.upm.api.domain.model.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "goa-user",
        url = "${goa.user.base-url}",
        configuration = FeignConfig.class
)
public interface UserWebClient {
    String ID_ID = "/{id}";

    @GetMapping(ID_ID)
    UserDto readById(@PathVariable String id);
}
