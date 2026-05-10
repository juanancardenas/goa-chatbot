package es.upm.api.domain.ports.out;

import es.upm.api.domain.model.UserDto;

public interface UserClient {

    UserDto readById(String id);
}
