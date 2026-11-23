package com.ozerler.marble.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.ozerler.marble.dto.EmailTemplateDto;
import com.ozerler.marble.model.EmailTemplate;

/**
 * Copies email templates into DTOs. No calculation happens here.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EmailTemplateMapper {

    /**
     * Copies one template.
     *
     * @param template the entity
     * @return the DTO
     */
    EmailTemplateDto toDto(EmailTemplate template);
}
