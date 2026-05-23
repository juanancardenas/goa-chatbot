package es.upm.api.adapter.out.mongodb.repository;

import es.upm.api.adapter.out.mongodb.entity.EscalationEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface EscalationRepository extends MongoRepository<EscalationEntity, UUID> {
}
