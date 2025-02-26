package com.example.firstproject.ui.matching

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.firstproject.R
import com.example.firstproject.data.model.dto.request.SendJoinRequestDTO
import com.example.firstproject.data.model.dto.response.UserSuitableStudyDtoItem
import com.example.firstproject.ui.common.CommonTopBar
import com.example.firstproject.ui.home.JoinProfiles
import com.example.firstproject.ui.theme.pretendard
import com.example.firstproject.utils.TopicTagEnum
import kotlinx.coroutines.delay

@Composable
fun FindStudyScreen(
    navController: NavController,
    findViewModel: FindViewModel = viewModel()

) {
    val context = LocalContext.current

    val recommandStudyResult by findViewModel.recommandStudyResult.collectAsStateWithLifecycle()
    // 추천 스터디 리스트
    val recommandStudyList by findViewModel.recommandStudyList.collectAsStateWithLifecycle()


    // 통신 상태
    var isLoading by remember { mutableStateOf(false) }

    // 화면 시작할 때 통신 수행
    LaunchedEffect(Unit) {
        findViewModel.getRecommandStudyList()
    }

    LaunchedEffect(recommandStudyResult) {
        if (recommandStudyResult.isProgress()) {
            // 로딩 중
            isLoading = true
            delay(1000)
        } else if (recommandStudyResult.isSuccess()) {
            // 통신 성공
            isLoading = false
            Log.d("스터디 추천", "${recommandStudyResult}")
            Log.d("스터디 추천", "${recommandStudyList}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CommonTopBar("", onBackPress = {
            // 뒤로 가기
            navController.navigate("homeScreen")
        })

        Image(
            painter = painterResource(R.drawable.img_find_study), null,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .fillMaxHeight(0.07f)
        )
        Spacer(Modifier.height(36.dp))

        recommandStudyList.forEach { studyInfo ->
            StudyInfoItem(navController, studyInfo, findViewModel, context)
        }


    }

}

@Composable
fun StackLabel(stackTitle: String, tint: Color) {
    Box(
        modifier = Modifier
            .width(56.dp)
            .height(24.dp)
            .background(tint, RoundedCornerShape(50.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stackTitle,
            color = Color.White,
            fontFamily = pretendard,
            fontWeight = FontWeight(500),
            fontSize = 11.sp
        )

    }
}

@Composable
private fun StudyInfoItem(
    navController: NavController,
    studyInfo: UserSuitableStudyDtoItem,
    findViewModel: FindViewModel,
    context: Context
) {
    var isInviteSent by remember { mutableStateOf(false) }
    var isInviteLoading by remember { mutableStateOf(false) }

    fun onSendJoinClick() {
        isInviteLoading = true
        findViewModel.sendJoinStudy(SendJoinRequestDTO(studyInfo.studyId)) { success ->
            isInviteLoading = false
            if (success) {
                isInviteSent = true
            } else {
                Toast.makeText(context, "죄송합니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val joinStudyResult by findViewModel.joinStudyResult.collectAsStateWithLifecycle()

    LaunchedEffect(joinStudyResult) {
        when {
            joinStudyResult.isProgress() -> {
                Log.d("스터디 초대함", "로딩 중")
            }

            joinStudyResult.isSuccess() -> {
                Log.d("스터디 초대함", "초대 성공")
//                isInviteSent = true
            }

            joinStudyResult.isFailure() -> {
                Log.d("스터디 초대함", "실패 ${joinStudyResult}")

                Toast.makeText(context, "죄송합니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val weekList = studyInfo.meetingDays

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val label = TopicTagEnum.fromTitle(studyInfo.topic)
            if (label != null) {
                StackLabel(
                    stackTitle = label.title,
                    tint = colorResource(label.colorId)
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = studyInfo.studyName,
                fontFamily = pretendard,
                fontWeight = FontWeight(700),
                fontSize = 20.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth(0.7f),
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .height(24.dp)
                    .clickable {
                        // 클릭하면 스터디 상세정보화면으로
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("studyId", studyInfo.studyId)
                        Log.d("전체 스터디 목록에서 누름", "스터디아이디: ${studyInfo.studyId}")

                        navController.navigate("studyDetailScreen")
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "자세히 보기",
                    fontFamily = pretendard,
                    fontWeight = FontWeight(400),
                    fontSize = 13.sp
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_right_arrow),
                    null,
                    Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
        }
        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp)
        ) {
            Text(
                text = "스터디 요일 :",
                fontFamily = pretendard,
                fontWeight = FontWeight(600),
                fontSize = 15.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = weekList.joinToString(" "),
                fontFamily = pretendard,
                fontWeight = FontWeight(600),
                fontSize = 16.sp,
                letterSpacing = 2.sp,
                color = Color(0xFF1181F0)
            )
        }
        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 28.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {

            JoinProfiles(studyInfo.memberCount, studyInfo.members) // 스터디 참여중인 사람 수 넣기

            Spacer(Modifier.width(16.dp))
            Text(
                text = "인원 :  ",
                fontFamily = pretendard,
                fontWeight = FontWeight(600),
                fontSize = 16.sp,
            )
            Text(
                text = "${studyInfo.memberCount} / ${studyInfo.count}명",
                fontFamily = pretendard,
                fontWeight = FontWeight(600),
                fontSize = 16.sp,
            )

        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            if (isInviteSent) {
                CompleteRequestButton()
            } else {
                SendRequestButton(
                    onClick = { onSendJoinClick() }
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Divider(color = Color(0xFF949494))
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SendRequestButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(84.dp)
            .height(28.dp)
            .background(color = Color.Black, shape = RoundedCornerShape(50.dp))
            .clickable {
                // 스터디 신청 통신 요청
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_add_plus),
                null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Text(
                "가입 신청", fontFamily = pretendard,
                fontWeight = FontWeight(700),
                fontSize = 13.sp,
                color = Color.White
            )
            Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
private fun CompleteRequestButton() {
    Box(
        modifier = Modifier
            .width(84.dp)
            .height(28.dp)
            .background(color = Color(0xFF15CD6C), shape = RoundedCornerShape(50.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Text(
                "신청 완료", fontFamily = pretendard,
                fontWeight = FontWeight(700),
                fontSize = 13.sp,
                color = Color.White
            )
            Spacer(Modifier.width(4.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TEST() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CommonTopBar("", onBackPress = {
            // 뒤로 가기
        })

        Image(
            painter = painterResource(R.drawable.img_find_study), null,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .fillMaxHeight(0.08f)

        )
        Spacer(Modifier.height(36.dp))
    }
}