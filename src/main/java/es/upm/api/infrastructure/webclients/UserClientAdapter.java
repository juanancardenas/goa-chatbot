package es.upm.api.infrastructure.webclients;

import es.upm.api.domain.model.platform.UserSummary;
import es.upm.api.domain.ports.out.UserClient;
import org.springframework.stereotype.Component;

@Component
public class UserClientAdapter implements UserClient {

    private final UserFeignClient userFeignClient;

    public UserClientAdapter(UserFeignClient userFeignClient) {
        this.userFeignClient = userFeignClient;
    }

    @Override
    public UserSummary readById(String id) {
        return this.userFeignClient.readById(id);
    }
}
