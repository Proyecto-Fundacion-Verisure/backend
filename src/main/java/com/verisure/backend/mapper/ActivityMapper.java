package com.verisure.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.verisure.backend.dto.activity.ActivityFormResponse;
import com.verisure.backend.dto.activity.ActivityResponse;
import com.verisure.backend.dto.activity.CreateActivityRequest;
import com.verisure.backend.dto.activity.UpdateActivityRequest;
import com.verisure.backend.entity.Activity;

@Mapper(componentModel = "spring")
public interface ActivityMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "DRAFT")
    Activity toEntity(CreateActivityRequest request);

    @Mapping(source = "partner.name", target = "partnerName")
    ActivityResponse toResponse(Activity activity);

    @Mapping(source = "partner.name", target = "partnerName")
    ActivityFormResponse toFormResponse(Activity activity);

    /**
     * Copia los campos editables sobre la entidad ya cargada, dejando intacto
     * todo lo que el formulario no escribe: {@code id}, {@code status},
     * {@code partner}, {@code createdBy} y {@code reviewNote}.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "partner", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "reviewNote", ignore = true)
    @Mapping(target = "registrations", ignore = true)
    @Mapping(target = "favorites", ignore = true)
    void updateEntity(UpdateActivityRequest request, @MappingTarget Activity activity);

    // El catálogo no pasa por aquí: ActivityCardResponse lleva dos campos
    // calculados —plazas cubiertas y «me gusta» de quien mira— que salen de
    // consultas distintas, y eso un mapper no lo sabe hacer. Los monta
    // ActivityCatalogService · B2-07.

}
