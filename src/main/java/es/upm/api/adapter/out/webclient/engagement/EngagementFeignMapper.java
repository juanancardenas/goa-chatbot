package es.upm.api.adapter.out.webclient.engagement;

import es.upm.api.domain.model.platform.EngagementEventPage;
import es.upm.api.domain.model.platform.EngagementEventSummary;
import es.upm.api.domain.model.platform.EngagementLetterSummary;
import es.upm.api.domain.model.platform.LegalProcedureSummary;
import es.upm.api.adapter.out.webclient.engagement.dto.EngagementEventPageResponseDto;
import es.upm.api.adapter.out.webclient.engagement.dto.EngagementEventResponseDto;
import es.upm.api.adapter.out.webclient.engagement.dto.EngagementLetterResponseDto;
import es.upm.api.adapter.out.webclient.user.UserFeignMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EngagementFeignMapper {

    private final UserFeignMapper userFeignMapper;

    public EngagementFeignMapper(UserFeignMapper userFeignMapper) {
        this.userFeignMapper = userFeignMapper;
    }

    public EngagementLetterSummary toDomain(EngagementLetterResponseDto dto) {
        if (dto == null) {
            return null;
        }

        return new EngagementLetterSummary(
                dto.getId(),
                dto.getCreationDate(),
                dto.getClosingDate(),
                this.userFeignMapper.toDomain(dto.getOwner()),
                this.mapLegalProcedures(dto.getLegalProcedures())
        );
    }

    public EngagementEventPage toDomain(EngagementEventPageResponseDto dto) {
        if (dto == null) {
            return null;
        }

        return new EngagementEventPage(this.mapEvents(dto.getContent()));
    }

    private List<LegalProcedureSummary> mapLegalProcedures(
            List<EngagementLetterResponseDto.LegalProcedureResponseDto> legalProcedures
    ) {
        if (legalProcedures == null) {
            return List.of();
        }

        return legalProcedures.stream()
                .filter(procedure -> procedure != null)
                .map(procedure -> new LegalProcedureSummary(
                        procedure.getTitle(),
                        procedure.getStartDate(),
                        procedure.getClosingDate(),
                        procedure.getLegalTasks() != null ? procedure.getLegalTasks() : List.of()
                ))
                .toList();
    }

    private List<EngagementEventSummary> mapEvents(List<EngagementEventResponseDto> events) {
        if (events == null) {
            return List.of();
        }

        return events.stream()
                .filter(event -> event != null)
                .map(event -> new EngagementEventSummary(
                        event.getType(),
                        event.getState(),
                        event.getTitle(),
                        event.getComment(),
                        event.getDate()
                ))
                .toList();
    }
}
