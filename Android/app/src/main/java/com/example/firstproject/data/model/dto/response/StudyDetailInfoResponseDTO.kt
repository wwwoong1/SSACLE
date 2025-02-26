package com.example.firstproject.data.model.dto.response

// GET/api/studies/{studyId} 특정 스터디 조회
data class StudyDetailInfoResponseDTO(
    val count: Int,
    val createdBy: String,
    val feeds: List<Feed>,
    val id: String,
    val meetingDays: List<String>,
    val memberCont: Int,
    val members: List<Member>,
    val studyContent: String,
    val studyName: String,
    val topic: String
)