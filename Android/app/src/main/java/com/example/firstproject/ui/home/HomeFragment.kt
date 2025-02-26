package com.example.firstproject.ui.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.os.bundleOf
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.findNavController
import com.example.firstproject.R
import com.example.firstproject.databinding.FragmentHomeBinding
import com.example.firstproject.ui.matching.ChooseStudyScreen
import com.example.firstproject.ui.matching.FindPersonScreen
import com.example.firstproject.ui.matching.FindStudyScreen
import com.example.firstproject.ui.matching.RegisterStudyScreen

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val navController = rememberNavController()
                val xmlNavController = findNavController()

                // HomeFragment로 전달된 studyId (RequestListFragment에서 Bundle로 보낸 값)
                val passedStudyId = arguments?.getString("studyId")
                // 만약 studyId가 전달되었다면, 현재 NavController의 백스택 Entry(savedStateHandle)에 저장
                LaunchedEffect(passedStudyId) {
                    if (!passedStudyId.isNullOrEmpty()) {
                        navController.currentBackStackEntry?.savedStateHandle?.set("studyId", passedStudyId)
                        navController.navigate("studyDetailScreen")
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "homeScreen"
                ) {
                    composable("homeScreen") {
                        HomeScreen(
                            navController = navController,
                            onNavigateToFragment = {
                                xmlNavController.navigate(R.id.action_homeFragment_to_studyRegisterFragment)
                            },
                            onNotificationClick = {
                                xmlNavController.navigate(R.id.action_homeFragment_to_notificationFragment)
                            }
                        )
                    }


                    composable("studyDetailScreen") {
//                        val studyId = navController.previousBackStackEntry
//                            ?.savedStateHandle
//                            ?.get<String>("studyId") ?: ""
                        StudyDetailScreen(
                            navController = navController,
//                            id = studyId,
                            onNavigateToVideo = { studyId, studyName ->
                                val bundle = bundleOf("studyId" to studyId, "studyName" to studyName)
                                xmlNavController.navigate(R.id.action_homeFragment_to_videoFragment, bundle)
                            },
                            onNavigateToChat = { studyId ->
                                val bundle = bundleOf("studyId" to studyId)
                                xmlNavController.navigate(R.id.action_homeFragment_to_chatFragment, bundle)
                            },
                            onNotificationClick = { studyId ->
                                val bundle = bundleOf("studyId" to studyId)
                                xmlNavController.navigate(R.id.action_homeFragment_to_studyNotificationFragment, bundle)
                            }

                        )
                    }

                    composable("allStudyListScreen") {
                        AllStudyListScreen(
                            navController = navController
                        )
                    }

                    composable("findStudyScreen") {
                        FindStudyScreen(navController = navController)
                    }

                    composable("chooseStudyScreen") {
                        ChooseStudyScreen(navController = navController)
                    }

                    composable("findPersonScreen") {
                        FindPersonScreen(navController = navController)
                    }

                }
            }
        }

    }

    companion object {
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}