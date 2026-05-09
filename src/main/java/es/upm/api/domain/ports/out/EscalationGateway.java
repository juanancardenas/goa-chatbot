package es.upm.api.domain.ports.out;

import es.upm.api.domain.model.Escalation;
import org.springframework.stereotype.Repository;

@Repository
public interface EscalationGateway {

    void create(Escalation escalation);

}
