package es.upm.api.domain.ports.out;

import es.upm.api.domain.model.UserDto;

public interface UserClientFinder {

    UserDto readById(String id);
}
