package S12P11D110.ssacle.domain.study.controller;

import S12P11D110.ssacle.domain.auth.entity.CustomUserDetail;
import S12P11D110.ssacle.domain.study.dto.request.AcceptUser;
import S12P11D110.ssacle.domain.study.dto.response.WishPreMembers;
import S12P11D110.ssacle.domain.study.dto.request.StudyCreateRequest;
import S12P11D110.ssacle.domain.study.dto.request.StudyRequest;
import S12P11D110.ssacle.domain.study.dto.response.*;
import S12P11D110.ssacle.domain.study.service.StudyService;
import S12P11D110.ssacle.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;


@RestController //RESTful API 개발에 사용
@RequiredArgsConstructor
@RequestMapping("/api/studies")
@Tag(name = "Study Controller", description = "This is Study Controller")
public class StudyController {

    private final StudyService studyService;

    // GPT: 25 ~31
    // 스터디 생성 POST
    @PostMapping("")
    @Operation(summary = "스터디 개설", description = "새로운 스터디를 개설합니다.")
    public ResponseEntity<Void> createStudy(@AuthenticationPrincipal CustomUserDetail userDetail, @RequestBody StudyCreateRequest studyCreateRequest) {  // 클라이언트로부터 전달받은 JSON 형식의 데이터를 @RequestBody를 통해 Java의 객체(StudyCreateRequestDTO)로 자동 변환
        String userId = userDetail.getId();
        studyService.saveStudy(userId, studyCreateRequest);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/recruitingStudy")
    @Operation(summary = "모집 중인 스터디 리스트", description = "모집중인 스터디의 리스트를 보여줍니다.")
    public List<RecrutingStudy> getRecruitingStudy(@AuthenticationPrincipal CustomUserDetail userDetail) {
        return studyService.recruitingStudy();
    }


    // 스터디 상세보기 GET
    @GetMapping("/{studyId}")
    @Operation(summary = "스터디 상세 정보", description = "스터디 상세 정보를 볼 수 있다.")
    public StudyDetail getstudyById(@PathVariable String studyId) {
        return studyService.getStudyById(studyId);
    }


//-------------------<< 스터디원 추천 기능>>-------------------------------------------------------------------------------
    // 스터디원 추천 기능
    @GetMapping("/recommendUser/{studyId}")
    @Operation(summary = "스터디원 추천기능", description = "스터디의 조건에 맞는 유저를 추천해준다.")
    public List<RecommendUser> getRecommendUser(@PathVariable String studyId) {
        return studyService.getStudyCondition(studyId);
    }

    // 스터디내 초대 현황 wishMembers & 내 수신함  invitedStudy 추가
    @PatchMapping("/{studyId}/addStudyRequest")
    @Operation(summary = "유저 스카웃(가입 요청)", description = "추천된 유저에게 스카웃 제의를 보낸다")
    //ResponseEntity :  HTTP 응답을 표현하는 클래스
    public ResponseEntity<Void> comeToStudy(@PathVariable String studyId, @RequestBody StudyRequest request) {
        studyService.addWishMemberInvitedStudy(studyId, request.getUserId());
        return ResponseEntity.ok().build();
    }
//----------------------------------------------------------------------------------------------------------------------


//-------------------<< 스터디 추천 기능>>-------------------------------------------------------------------------------
    // 스터디 추천기능
    @GetMapping("/recommendStudy") // user 로그인 정보 받아와지면 {userId} 없애기
    @Operation(summary = "스터디 추천기능", description = "유저의 조건에 맞는 스터디를 추천해준다.")
    public List<RecommendStudy> getRecommendStudy(@AuthenticationPrincipal CustomUserDetail userDetail) {
        String userId = userDetail.getId();
        return studyService.getUserCondition(userId);
    }


//----------------------------------------------------------------------------------------------------------------------


//--------------------<<  스터디 수신함   >>------------------------------------------------------------------------------
    @GetMapping("/{studyId}/wishList") //
    @Operation(summary = "스터디내 초대 현황", description = "스터디에서 초대 요청한 스터디원의 목록을 볼 수 있다")
    public List<WishPreMembers> getStudyWishMembersList(String studyId) {
        return studyService.studyWishMembersList(studyId);
    }

    @GetMapping("/{studyId}/preList") //
    @Operation(summary = "스터디내 수신함", description = "스터디에 가입 요청한 사람들의 목록을 확인 할 수 있다.")
    public List<WishPreMembers> getStudyPreMembersList(String studyId) {
        return studyService.studyPreMembersList(studyId);
    }

//----------------------------------------------------------------------------------------------------------------------

    // 스터디: 유저의 요청 수락
    @PatchMapping("/{studyId}/accept")
    @Operation(summary = "유저의 요청 수락", description = "스터디에 가입 요청한 사람의 가입 신청을 수락할 수 있다.")
    public ResponseEntity<Void> acceptInvite(@PathVariable String studyId, @RequestBody AcceptUser request) {
        studyService.addJoinedStudyMember(request.getUserId(), studyId);
        studyService.editWishStudyPreMembers(request.getUserId(), studyId);
        return ResponseEntity.ok().build();
    }

}









// GTP : 37 ~ 46
//    // 해당 조건의 스터디 그룹 조회
//    @GetMapping
//    @Operation(summary = "조건부 스터디 조회", description = "주제와 모임 요일 조건에 따라 스터디를 조회합니다. 조건이 없으면 전체 스터디를 반환합니다.")
//    public List<StudyResponseDTO> getStudiesByConditions(
//            @RequestParam(required = false) List<Study.Topic> topic, // 쿼리 파라미터 topic
//            @RequestParam(required = false) List<Study.MeetingDay> meetingDay // 쿼리 파라미터 meetingDay
//    ) {
//        return studyService.getStudiesByConditions(topic, meetingDay);
//    }



//
//
//    // 전체 스터디 조회 GET
//    @GetMapping
//    @Operation(summary = "모든 스터디 리스트", description = "개설된 모든 스터디 리스트 또는 추천된 스터디를 볼 수 있다.")
//    public List<StudyResponse> getAllStudies(){
//        return studyService.getAllStudy();
//    }
//


//    // uploads 파일 확인
//    private static final String UPLOADS_DIR = "uploads"; // 기본 폴더 경로
//    @GetMapping("/upload")
//    @Operation(summary = "uploads 폴더를 찾아주는 주문", description = "폴더야 어디있니")
//    public String getUploadsFolderPath() {
//        File folder = new File(UPLOADS_DIR);
//        return folder.getAbsolutePath();
//    }
//}

    // GTP : 37 ~ 46
//    // 해당 조건의 스터디 그룹 조회
//    @GetMapping
//    @Operation(summary = "조건부 스터디 조회", description = "주제와 모임 요일 조건에 따라 스터디를 조회합니다. 조건이 없으면 전체 스터디를 반환합니다.")
//    public List<StudyResponseDTO> getStudiesByConditions(
//            @RequestParam(required = false) List<Study.Topic> topic, // 쿼리 파라미터 topic
//            @RequestParam(required = false) List<Study.MeetingDay> meetingDay // 쿼리 파라미터 meetingDay
//    ) {
//        return studyService.getStudiesByConditions(topic, meetingDay);
//    }





    // 스터디 수정
    // 스터디 삭제





