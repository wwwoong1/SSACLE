package com.example.firstproject.data.repository

import android.util.Log
import com.example.firstproject.BuildConfig
import com.example.firstproject.MyApplication
import com.example.firstproject.data.model.dto.request.AuthRequestDTO
import com.example.firstproject.data.model.dto.request.EditProfileRequestDTO
import com.example.firstproject.data.model.dto.request.InviteUserRequestDTO
import com.example.firstproject.data.model.dto.request.NicknameRequestDTO
import com.example.firstproject.data.model.dto.request.RegisterStudyRequestDTO
import com.example.firstproject.data.model.dto.request.SendJoinRequestDTO
import com.example.firstproject.data.model.dto.response.AuthResponseDTO
import com.example.firstproject.data.model.dto.response.EditProfileResponseDTO
import com.example.firstproject.data.model.dto.response.KakaoTokenDTO
import com.example.firstproject.data.model.dto.response.MyAppliedStudyListDtoItem
import com.example.firstproject.data.model.dto.response.MyInvitedStudyListDtoItem
import com.example.firstproject.data.model.dto.response.MyJoinedStudyListDtoItem
import com.example.firstproject.data.model.dto.response.Profile
import com.example.firstproject.data.model.dto.response.RefreshTokenDTO
import com.example.firstproject.data.model.dto.response.StudyDTO
import com.example.firstproject.data.model.dto.response.StudyDetailInfoResponseDTO
import com.example.firstproject.data.model.dto.response.StudyJoinRequestListDtoItem
import com.example.firstproject.data.model.dto.response.StudyRequestedInviteListDtoItem
import com.example.firstproject.data.model.dto.response.Top3RecommendedUsersDtoItem
import com.example.firstproject.data.model.dto.response.UserSuitableStudyDtoItem
import com.example.firstproject.data.model.dto.response.common.CommonResponseDTO
import com.example.firstproject.network.APIService
import com.google.gson.Gson
import com.rootachieve.requestresult.RequestResult
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit


private val TAG = "리모트데이터소스"

class RemoteDataSource {
    private val context = MyApplication.appContext

    companion object {
        private const val BASE_URL_SPRING = "http://43.203.250.200:5001/"
        private const val BASE_URL_RTC = ""
        private const val BASE_URL_CHAT = ""
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        redactHeader("Authorization")
        redactHeader("Cookie")
    }

    private val tokenInterceptor = TokenInterceptor(context)

    private val client = OkHttpClient.Builder()
        .addInterceptor(tokenInterceptor) // 토큰 인터셉터 추가
        .addInterceptor(loggingInterceptor) // 로깅 인터셉터 추가
        .connectTimeout(15, TimeUnit.SECONDS) // 연결 타임아웃
        .readTimeout(15, TimeUnit.SECONDS)    // 읽기 타임아웃
        .writeTimeout(15, TimeUnit.SECONDS)  // 쓰기 타임아웃
        .build()

    private val retrofitSpring: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_SPRING)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    private val retrofitRTC: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_RTC)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    private val retrofitChat: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_CHAT)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    fun getSpringService(): APIService {
        return retrofitSpring.create(APIService::class.java)
    }

    fun getRTCService(): APIService {
        return retrofitRTC.create(APIService::class.java)
    }

    fun getChatService(): APIService {
        return retrofitChat.create(APIService::class.java)
    }

    private val springService = getSpringService()

    suspend fun loginWithKakao(accessToken: String): RequestResult<KakaoTokenDTO> {
        Log.d(TAG, "서버로 보낼 토큰: Bearer $accessToken")
        return try {
            val response = springService.kakaoLogin("Bearer $accessToken")
            Log.d(TAG, "서버 응답 코드: ${response.code()}") // ✅ HTTP 응답 코드 확인
            Log.d(TAG, "서버 응답 바디: ${response.body()}") // ✅ 응답 바디 로그

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (response.code() == 200 && body.data != null) {
                    Log.d(TAG, "서버 응답 성공: ${body.code} - ${body.message}")
                    RequestResult.Success(body.data)  // ✅ KakaoTokenDTO 반환

                } else {
                    Log.e(TAG, "서버에서 로그인 실패: ${body.code} - ${body.message}")

                    RequestResult.Failure(
                        body.code.toString(),
                        Exception(body.message ?: "로그인 실패")
                    )
                }

            } else {
                Log.e(TAG, "서버 응답 실패: ${response.code()}")

                RequestResult.Failure(response.code().toString(), Exception("서버 응답 실패"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "로그인 요청 중 예외 발생", e)
            RequestResult.Failure("EXCEPTION", e)
        }
    }

    suspend fun refreshAccessToken(refreshToken: String): RequestResult<RefreshTokenDTO> {
        return try {
            val response = springService.getRefreshToken("Bearer $refreshToken")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()
                if (body != null && body.code == 200 && body.data != null) {
                    RequestResult.Success(body.data)  // ✅ RefreshTokenDTO 반환
                } else {
                    RequestResult.Failure(
                        body?.code.toString(),
                        Exception(body?.message ?: "토큰 갱신 실패")
                    )
                }
            } else {
                RequestResult.Failure(response.code().toString(), Exception("서버 응답 실패"))
            }

        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }

    // 이건 아님
//    suspend fun getAllStudyList(accessToken: String): RequestResult<AllStudyListResponseDTO> {
//        return try {
//            val response = springService.getAllStudies("Bearer $accessToken")
//
//            if (response.isSuccessful && response.body() != null) {
//                val body = response.body()!!
//
//                if (response.code() == 200 && body.data != null) {
//                    RequestResult.Success(body.data)
//                } else {
//                    RequestResult.Failure(
//                        body.code.toString(),
//                        Exception(body.message ?: "로그인 실패")
//                    )
//                }
//
//            } else {
//                RequestResult.Failure(response.code().toString(), Exception("서버 응답 실패"))
//            }
//        } catch (e: Exception) {
//            RequestResult.Failure("EXCEPTION", e)
//        }
//    }

    // 사용자 싸피생 인증
    suspend fun AuthUser(
        accessToken: String,
        request: AuthRequestDTO
    ): RequestResult<CommonResponseDTO<AuthResponseDTO>> {
        return try {
            val response = springService.AuthUser("Bearer $accessToken", request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                RequestResult.Success(body)
            } else {
                RequestResult.Failure(response.code().toString(), Exception("통신 실패"))
            }
        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }

    // 닉네임 중복확인
    suspend fun CheckNickName(
        accessToken: String,
        nickname: NicknameRequestDTO
    ): RequestResult<CommonResponseDTO<Boolean>> {
        return try {
            val response = springService.CheckNickName("Bearer $accessToken", nickname)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                RequestResult.Success(body)
            } else {
                RequestResult.Failure(response.code().toString(), Exception("통신 실패"))
            }

        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }

    // 온보딩 등록
    suspend fun OnboardingProfile(
        accessToken: String,
        request: EditProfileRequestDTO,
        imageFile: File?
    ): RequestResult<EditProfileResponseDTO> {
        return try {
            // 1) JSON 문자열로 변환
            val jsonString = Gson().toJson(request)
            val jsonRequestBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())

            // 2) 파일을 MultipartBody.Part로
            //    서버가 @RequestParam("MultipartFile")로 받으니, 파트 이름은 "MultipartFile"로
            val filePart = if (imageFile != null && imageFile.exists()) {
                val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("MultipartFile", imageFile.name, requestFile)
            } else {
                // 파일이 없으면 빈 문자열로 보냄
                val emptyBody = "".toRequestBody("text/plain".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("MultipartFile", "", emptyBody)
            }


            val response =
                springService.OnboardingProfile("Bearer $accessToken", jsonRequestBody, filePart)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                RequestResult.Success(body)
            } else {

                RequestResult.Failure(
                    code = response.code().toString(),
                    exception = Exception(response.errorBody()?.string() ?: "통신 실패")
                )
            }

        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }


    // 스터디 관련 통신
    // 모집 중인 스터디 조회
    suspend fun getAllStudy(accessToken: String): RequestResult<List<StudyDTO>> {
        return try {
            val response = springService.getAllStudies("Bearer $accessToken")
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                RequestResult.Success(body)
            } else {
                RequestResult.Failure(response.code().toString(), Exception("통신 실패"))
            }

        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }

    // /api/studies/{studyId} 특정 스터디 조회
    suspend fun getStudyDetailInfo(
        accessToken: String,
        studyId: String
    ): RequestResult<StudyDetailInfoResponseDTO> {
        return try {
            val response = springService.getStudyDetailInfo("Bearer $accessToken", studyId)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                RequestResult.Success(body)
            } else {
                RequestResult.Failure(response.code().toString(), Exception("통신 실패"))
            }

        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }

    // 스터디 개설
    suspend fun sendRegisterStudy(
        accessToken: String,
        request: RegisterStudyRequestDTO
    ): RequestResult<Unit> {
        return try {
            val response = springService.registerStudy("Bearer $accessToken", request)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                RequestResult.Success(body)
            } else {
                RequestResult.Failure(response.code().toString(), Exception("통신 실패"))
            }

        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }

    // /api/studies/{studyId}/wishList 스터디내 초대 현황
    suspend fun getStudyInvitedMembers(
        accessToken: String,
        studyId: String
    ): RequestResult<List<StudyRequestedInviteListDtoItem>> {
        return try {
            val response = springService.getStudyInvitedMembers("Bearer $accessToken", studyId)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                RequestResult.Success(body)
            } else {
                RequestResult.Failure(response.code().toString(), Exception("통신 실패"))
            }

        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }

    // /api/studies/{studyId}/preList 스터디내 수신함
    suspend fun getStudyJoinRequests(
        accessToken: String,
        studyId: String
    ): RequestResult<List<StudyJoinRequestListDtoItem>> {
        return try {
            val response = springService.getStudyJoinRequests("Bearer $accessToken", studyId)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                RequestResult.Success(body)
            } else {
                RequestResult.Failure(response.code().toString(), Exception("통신 실패"))
            }

        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }

    // /api/studies/recommendUser/{studyId} 스터디원 추천
    suspend fun getTop3StudyCandidates(
        accessToken: String,
        studyId: String
    ): RequestResult<List<Top3RecommendedUsersDtoItem>> {
        return try {
            val response = springService.getTop3StudyCandidates("Bearer $accessToken", studyId)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                RequestResult.Success(body)
            } else {
                RequestResult.Failure(response.code().toString(), Exception("통신 실패"))
            }

        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }

    // /api/studies/recommendStudy 스터디 추천기능
    suspend fun getRecommendedStudies(accessToken: String): RequestResult<List<UserSuitableStudyDtoItem>> {
        return try {
            val response = springService.getRecommendedStudies("Bearer $accessToken")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                RequestResult.Success(body)
            } else {
                RequestResult.Failure(response.code().toString(), Exception("통신 실패"))
            }

        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }

    // /api/user 로그아웃
    suspend fun logout(accessToken: String): RequestResult<CommonResponseDTO<Unit>> {
        return try {
            val response = springService.logout("Bearer $accessToken")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                RequestResult.Success(body)
            } else {
                RequestResult.Failure(response.code().toString(), Exception("통신 실패"))
            }

        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }

    // /api/user 회원탈퇴
    suspend fun deleteUserAccount(accessToken: String): RequestResult<CommonResponseDTO<Unit>> {
        return try {
            val response = springService.deleteUserAccount("Bearer $accessToken")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                RequestResult.Success(body)
            } else {
                RequestResult.Failure(response.code().toString(), Exception("통신 실패"))
            }

        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }

    // /api/user/wish-studies 내 신청 현황 리스트
    suspend fun getMyAppliedStudies(accessToken: String): RequestResult<List<MyAppliedStudyListDtoItem>> {
        return try {
            val response = springService.getMyAppliedStudies("Bearer $accessToken")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                RequestResult.Success(body)
            } else {
                RequestResult.Failure(response.code().toString(), Exception("통신 실패"))
            }

        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }

    // /api/user/profile 프로필 조회
    suspend fun getUserProfile(accessToken: String): RequestResult<CommonResponseDTO<Profile>> {
        return try {
            val response = springService.getUserProfile("Bearer $accessToken")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                RequestResult.Success(body)
            } else {
                RequestResult.Failure(response.code().toString(), Exception("통신 실패"))
            }

        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }

    // /api/user/my-studies 내 스터디 리스트
    suspend fun getMyJoinedStudies(accessToken: String): RequestResult<List<MyJoinedStudyListDtoItem>> {
        return try {
            val response = springService.getMyJoinedStudies("Bearer $accessToken")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                RequestResult.Success(body)
            } else {
                RequestResult.Failure(response.code().toString(), Exception("통신 실패"))
            }

        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }

    // /api/user/invited-studies 내 수신함
    suspend fun getMyInvitedStudies(accessToken: String): RequestResult<List<MyInvitedStudyListDtoItem>> {
        return try {
            val response = springService.getMyInvitedStudies("Bearer $accessToken")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                RequestResult.Success(body)
            } else {
                RequestResult.Failure(response.code().toString(), Exception("통신 실패"))
            }

        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }

    fun getImageUrl(url: String): String {
        return BASE_URL_SPRING + "images/" + url
    }

    // 유저에게 스터디 초대 보내기
    suspend fun inviteStudyToUser(
        accessToken: String,
        studyId: String,
        request: InviteUserRequestDTO
    ): RequestResult<Unit> {
        return try {
            val response = springService.inviteStudyToUser("Bearer $accessToken", studyId, request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                RequestResult.Success(body)
            } else {
                RequestResult.Failure(response.code().toString(), Exception("통신 실패"))
            }

        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }

    suspend fun sendJoinRequest(
        accessToken: String,
        studyId: SendJoinRequestDTO
    ): RequestResult<Unit> {
        return try {
            val response = springService.sendJoinRequest("Bearer $accessToken", studyId)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                RequestResult.Success(body)
            } else {
                RequestResult.Failure(response.code().toString(), Exception("통신 실패"))
            }

        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }

    // 스터디에 온 가입 요청 수락
    suspend fun AcceptJoinUser(
        accessToken: String,
        studyId: String,
        request: InviteUserRequestDTO
    ): RequestResult<Unit> {
        return try {
            val response = springService.AcceptJoinUser("Bearer $accessToken", studyId, request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                RequestResult.Success(body)
            } else {
                RequestResult.Failure(response.code().toString(), Exception("통신 실패"))
            }

        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }

    // 나에게 온 스터디 초대 수락
    suspend fun acceptJoinStudy(
        accessToken: String,
        request: SendJoinRequestDTO
    ) : RequestResult<Unit> {
        return try {
            val response = springService.acceptJoinStudy("Bearer $accessToken", request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                RequestResult.Success(body)
            } else {
                RequestResult.Failure(response.code().toString(), Exception("통신 실패"))
            }

        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }

    // 프로필 수정
    suspend fun editUserProfile(
        accessToken: String,
        request: EditProfileRequestDTO,
        imageFile: File?
    ): RequestResult<CommonResponseDTO<EditProfileResponseDTO>> {
        return try {
            val jsonString = Gson().toJson(request)
            val jsonRequestBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())

            val filePart = if (imageFile != null && imageFile.exists()) {
                val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("MultipartFile", imageFile.name, requestFile)
            } else {
                null
            }


            val response =
                springService.editUserProfile("Bearer $accessToken", jsonRequestBody, filePart)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                RequestResult.Success(body)
            } else {
                RequestResult.Failure(
                    code = response.code().toString(),
                    exception = Exception(response.errorBody()?.string() ?: "통신 실패")
                )
            }
        } catch (e: Exception) {
            RequestResult.Failure("EXCEPTION", e)
        }
    }
}