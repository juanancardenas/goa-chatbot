package es.upm.api.infrastructure.webclients;

import es.upm.api.configurations.FeignConfig;
import es.upm.api.infrastructure.webclients.user.dto.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "goa-user",
        url = "${goa.user.base-url}",
        configuration = FeignConfig.class
)
public interface UserFeignClient {

    String ID_ID = "/{id}";

    @GetMapping(ID_ID)
    UserResponseDto readById(@PathVariable String id);
}
