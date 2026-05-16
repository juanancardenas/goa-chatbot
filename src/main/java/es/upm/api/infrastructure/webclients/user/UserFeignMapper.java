package es.upm.api.infrastructure.webclients.user;

import es.upm.api.domain.model.platform.UserSummary;
import es.upm.api.infrastructure.webclients.user.dto.UserResponseDto;
import org.springframework.stereotype.Component;

@Component
public class UserFeignMapper {

    public UserSummary toDomain(UserResponseDto dto) {
        if (dto == null) {
            return null;
        }

        return UserSummary.builder()
                .id(dto.getId())
                .firstName(dto.getFirstName())
                .familyName(dto.getFamilyName())
                .email(dto.getEmail())
                .mobile(dto.getMobile())
                .build();
    }
}
