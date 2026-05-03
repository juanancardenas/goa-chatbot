package es.upm.api.infrastructure.mongodb.persistence;

import es.upm.api.domain.model.Escalation;
import es.upm.api.domain.persistence.EscalationPersistence;
import es.upm.api.infrastructure.mongodb.daos.EscalationRepository;
import es.upm.api.infrastructure.mongodb.entities.EscalationEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class EscalationPersistenceMongodb implements EscalationPersistence {
    private final EscalationRepository escalationRepository;

    @Autowired
    public EscalationPersistenceMongodb(EscalationRepository escalationRepository) {
        this.escalationRepository = escalationRepository;
    }

    @Override
    public void create(Escalation escalation) {
        EscalationEntity entity = EscalationEntity.fromEscalation(escalation);
        this.escalationRepository.save(entity);
    }
}
