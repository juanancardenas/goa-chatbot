package es.upm.api.infrastructure.webclients;

import es.upm.api.domain.model.platform.UserSummary;
import es.upm.api.domain.ports.out.UserClient;
import es.upm.api.infrastructure.webclients.user.UserFeignMapper;
import org.springframework.stereotype.Component;

@Component
public class UserClientAdapter implements UserClient {

    private final UserFeignClient userFeignClient;
    private final UserFeignMapper userFeignMapper;

    public UserClientAdapter(
            UserFeignClient userFeignClient,
            UserFeignMapper userFeignMapper
    ) {
        this.userFeignClient = userFeignClient;
        this.userFeignMapper = userFeignMapper;
    }

    @Override
    public UserSummary readById(String id) {
        return this.userFeignMapper.toDomain(this.userFeignClient.readById(id));
    }
}
