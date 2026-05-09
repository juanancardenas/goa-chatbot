package es.upm.api.infrastructure.webclients;

import es.upm.api.configurations.FeignConfig;
import es.upm.api.domain.model.UserDto;
import es.upm.api.domain.ports.out.UserClientFinder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "goa-user",
        url = "${goa.user.base-url}",
        configuration = FeignConfig.class
)
public interface UserWebClient extends UserClientFinder {
    String ID_ID = "/{id}";

    @Override
    @GetMapping(ID_ID)
    UserDto readById(@PathVariable String id);
}
