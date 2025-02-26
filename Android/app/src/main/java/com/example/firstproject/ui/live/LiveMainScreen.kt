package com.example.firstproject.ui.live

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstproject.MyApplication
import com.example.firstproject.R
import com.example.firstproject.data.model.dto.response.MyJoinedStudyListDtoItem
import com.example.firstproject.ui.home.HomeViewModel
import com.example.firstproject.ui.home.JoinProfiles
import com.example.firstproject.ui.theme.pretendard
import com.example.firstproject.utils.TopicTagEnum
import com.rootachieve.requestresult.RequestResult

@Composable
fun LiveMainScreen(
    homeViewModel: HomeViewModel = viewModel(),
    onNavigateToVideo: (String, String) -> Unit,
) {
    val getJoinedStudyResult by homeViewModel.joinedStudyResult.collectAsStateWithLifecycle()
    val myJoinedStudyList = (getJoinedStudyResult as? RequestResult.Success)?.data

    // 통신 상태
    var isLoading by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        homeViewModel.getJoinedStudy()

    }

    LaunchedEffect(getJoinedStudyResult) {
        if (getJoinedStudyResult.isProgress()) {
            // 로딩 중
            isLoading = true
            Log.d("선택 화면", "로딩 중")
//            delay(1000)
        } else if (getJoinedStudyResult.isSuccess()) {
            // 통신 성공
            isLoading = false
            Log.d("선택 화면", "통신 완료")
        }

    }

    if (myJoinedStudyList != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {

            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(top = 8.dp)
            ) {
                items(myJoinedStudyList) { study ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .padding(horizontal = 24.dp)
                            .shadow(
                                elevation = 1.dp,
                                shape = RoundedCornerShape(10.dp),
                                clip = true,
                            )
                            .clickable {
                                // 클릭하면 라이브 방으로 이동
                                onNavigateToVideo(study.id, study.studyName)

                            },
                        shape = RoundedCornerShape(5.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(
                            1.dp,
                            colorResource(R.color.border_light_color)
                        )
                    ) {
                        StudyListItem(study)
                    }
                    Spacer(Modifier.height(28.dp))
                }


            }
        }
    } else {
        // 참여 중인 스터디가 없을 때 나타날 화면


    }

}


@Composable
private fun StudyListItem(studyInfo: MyJoinedStudyListDtoItem) {
    val isHost = studyInfo.createdBy == MyApplication.USER_ID

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                studyInfo.studyName,
                fontFamily = pretendard,
                fontWeight = FontWeight(600),
                fontSize = 20.sp,
                modifier = Modifier.fillMaxWidth(0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.weight(1f))

            // 방장인지 표시
            Box(modifier = Modifier.size(24.dp)) {
                if (isHost) {
                    Image(
                        painter = painterResource(R.drawable.icon_host), null,
                        modifier = Modifier
                            .size(24.dp)
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp)
        ) {
            Text(
                text = "스터디 요일 :",
                fontFamily = pretendard,
                fontWeight = FontWeight(500),
                fontSize = 15.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = studyInfo.meetingDays.joinToString(" "),
                fontFamily = pretendard,
                fontWeight = FontWeight(600),
                fontSize = 16.sp,
                letterSpacing = 2.sp,
                color = Color(0xFF1181F0)
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tag = TopicTagEnum.fromTitle(studyInfo.topic)
            if (tag != null) {
                ListStackLabel(stackTitle = tag.title, tint = colorResource(tag.colorId))
            }
            Spacer(Modifier.weight(1f))

            JoinProfiles(studyInfo.memberCount, studyInfo.members)
            Spacer(Modifier.width(16.dp))
            Text(
                text = "${studyInfo.memberCount}명 참여 중",
                color = Color(0xFF666666),
                fontFamily = pretendard,
                fontWeight = FontWeight(500),
                fontSize = 12.sp,
            )

        }


    }
}

@Composable
private fun ListStackLabel(stackTitle: String, tint: Color) {
    Box(
        modifier = Modifier
            .width(64.dp)
            .height(28.dp)
            .background(tint, RoundedCornerShape(50.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stackTitle,
            color = Color.White,
            fontFamily = pretendard,
            fontWeight = FontWeight(500),
            fontSize = 13.sp
        )

    }
}