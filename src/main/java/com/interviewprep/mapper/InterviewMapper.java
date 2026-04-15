package com.interviewprep.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewprep.dto.InterviewResponse;
import com.interviewprep.entity.Interview;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class InterviewMapper {

    @Autowired
    protected ObjectMapper objectMapper;

    @Mapping(target = "interviewId", source = "id")
    @Mapping(target = "config", expression = "java(parseConfig(interview.getConfig()))")
    public abstract InterviewResponse toResponse(Interview interview);

    protected InterviewResponse.InterviewConfig parseConfig(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return new InterviewResponse.InterviewConfig(10, "MEDIUM", "MEDIUM");
        }
        try {
            var node = objectMapper.readTree(configJson);
            return new InterviewResponse.InterviewConfig(
                    node.path("questionCount").asInt(10),
                    node.path("initialDifficulty").asText("MEDIUM"),
                    node.path("currentDifficulty").asText("MEDIUM")
            );
        } catch (JsonProcessingException e) {
            return new InterviewResponse.InterviewConfig(10, "MEDIUM", "MEDIUM");
        }
    }
}
