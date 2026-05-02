package es.upm.api.infrastructure.mongodb.daos;

import es.upm.api.infrastructure.mongodb.entities.EscalationEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface EscalationRepository extends MongoRepository<EscalationEntity, UUID> {
}
