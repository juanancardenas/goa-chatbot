package es.upm.api.domain.ports.out;

import es.upm.api.domain.model.Conversation;
import es.upm.api.domain.model.Escalation;

public interface EscalationGateway {

    void create(Escalation escalation);

    void createAndArchiveConversation(Conversation conversation, Escalation escalation);

}
