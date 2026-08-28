package com.verisure.backend.entity;

import java.time.LocalDate;

import org.aspectj.weaver.ast.Test;

import com.verisure.backend.entity.enums.ActivityStatus;

import jakarta.websocket.Decoder.Text;

public class Activity {


private Long id;

private String title;

private Text description;

private String line;

private String mode;

private String location;

private LocalDate startDate;

private LocalDate endDate;

private LocalDate registrationDeadline;

private Integer hours;

private Integer spots;

private String imageUrl;

private ActivityStatus status;

private Text reviewNote;


}
