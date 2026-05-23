package es.upm.api.adapter.out.webclient.user;

import es.upm.api.domain.model.platform.UserSummary;
import es.upm.api.domain.ports.out.UserClient;
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
