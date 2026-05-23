package es.upm.api.adapter.out.webclient.user;

import es.upm.api.configuration.FeignConfig;
import es.upm.api.adapter.out.webclient.user.dto.UserResponseDto;
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
