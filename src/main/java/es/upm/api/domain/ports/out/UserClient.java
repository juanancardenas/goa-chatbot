package es.upm.api.domain.ports.out;

import es.upm.api.domain.model.platform.UserSummary;

public interface UserClient {

    UserSummary readById(String id);
}
