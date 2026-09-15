package com.verisure.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.verisure.backend.dto.activity.ActivityResponse;
import com.verisure.backend.dto.activity.CreateActivityRequest;
import com.verisure.backend.entity.Activity;

@Mapper(componentModel = "spring")
public interface ActivityMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "DRAFT")
    Activity toEntity(CreateActivityRequest request);

    @Mapping(source = "partner.name", target = "partnerName")
    ActivityResponse toResponse(Activity activity);

    // El catálogo no pasa por aquí: ActivityCardResponse lleva dos campos
    // calculados —plazas cubiertas y «me gusta» de quien mira— que salen de
    // consultas distintas, y eso un mapper no lo sabe hacer. Los monta
    // ActivityCatalogService · B2-07.

}
