package es.upm.api.domain.persistence;

import es.upm.api.domain.model.Escalation;
import org.springframework.stereotype.Repository;

@Repository
public interface EscalationPersistence {

    void create(Escalation escalation);

}
