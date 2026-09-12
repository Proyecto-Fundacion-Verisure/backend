package com.verisure.backend.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.verisure.backend.dto.activity.ActivityCardResponse;
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

    List<ActivityCardResponse> toCardList(List<Activity> activities);

}
