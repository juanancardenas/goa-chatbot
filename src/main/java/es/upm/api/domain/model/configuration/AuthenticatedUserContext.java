package es.upm.api.domain.model.configuration;

import es.upm.api.domain.enums.ConversationProfileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedUserContext {

    private String userId;
    private ConversationProfileType profile;
}
