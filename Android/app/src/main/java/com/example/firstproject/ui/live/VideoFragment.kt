package com.example.firstproject.ui.live

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firstproject.MyApplication.Companion.NICKNAME
import com.example.firstproject.MyApplication.Companion.webRtcClientConnection
import com.example.firstproject.R
import com.example.firstproject.databinding.FragmentVideoBinding
import com.example.firstproject.dto.LiveChatMessage
import com.example.firstproject.dto.LiveMember
import kotlinx.coroutines.launch


class VideoFragment : Fragment() {

    companion object {
        const val TAG = "VideoFragment_TAG"
    }

    private var _binding: FragmentVideoBinding? = null
    private val binding get() = _binding!!

    private val liveMembers = mutableListOf<LiveMember>()
    private lateinit var liveMemberAdapter: LiveMemberAdapter

    // WebRTC 연결 객체
    private var peerId: String? = null
    lateinit var studyName: String
    lateinit var studyId: String

    private val liveChatMessages = mutableListOf<LiveChatMessage>()
    private lateinit var liveChatAdapter: LiveChatAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        studyId = arguments?.getString("studyId")!!
        Log.d(TAG, "onViewCreated: studyId= $studyId")
        studyName = arguments?.getString("studyName")!!
        Log.d(TAG, "onViewCreated: studyName= $studyName")

        initUI()
        webRtcClientConnection.init(requireContext())
        peerId = webRtcClientConnection.getSocket()?.id()

        liveMemberAdapter =
            LiveMemberAdapter(liveMembers, webRtcClientConnection.eglBase.eglBaseContext)
        binding.rvParticipants.adapter = liveMemberAdapter

        liveMembers.add(LiveMember(true, NICKNAME))
        lifecycleScope.launch { updatePeopleCount() }
        webRtcClientConnection.joinRoom(studyId)

        liveChatAdapter = LiveChatAdapter(liveChatMessages)
        binding.rvChat.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChat.adapter = liveChatAdapter

        // 원격 비디오 수신 시 처리
        webRtcClientConnection.onRemoteVideo = { _, consumer, nickname, peerId ->
            lifecycleScope.launch {
                val index = liveMembers.indexOfFirst { it.peerId == peerId }
                if (index != -1) {
                    liveMembers[index].videoConsumer = consumer
                } else {
                    liveMembers.add(LiveMember(false, nickname, peerId, videoConsumer = consumer))
                    updatePeopleCount()
                    liveMemberAdapter.notifyItemInserted(liveMembers.lastIndex)
                }
            }
        }
        webRtcClientConnection.onRemoteAudio = { _, consumer, nickname, peerId ->
            lifecycleScope.launch {
                val index = liveMembers.indexOfFirst { it.peerId == peerId }
                if (index != -1) {
                    liveMembers[index].videoConsumer = consumer
                } else {
                    liveMembers.add(LiveMember(false, nickname, peerId, audioConsumer = consumer))
                    updatePeopleCount()
                    liveMemberAdapter.notifyItemInserted(liveMembers.lastIndex)
                }
            }
        }

        // 상대방이 나갔을 때, 해당 멤버 삭제
        webRtcClientConnection.onPeerClosed = { peerId ->
            val index = liveMembers.indexOfFirst { it.peerId == peerId }
            Log.d(TAG, "closed index=$index, peerId=$peerId")
            if (index != -1) {
                liveMembers[index].videoConsumer?.close()
                liveMembers[index].audioConsumer?.close()

                liveMembers.removeAt(index)
                lifecycleScope.launch {
                    updatePeopleCount()
                    liveMemberAdapter.notifyItemRemoved(index)
                }
            }
        }

        webRtcClientConnection.onNewChat = { nickname, message ->
            lifecycleScope.launch {
                displayChatMessage(false, nickname, message)
            }
        }

    }

    private fun initUI() {

        // Toolbar 설정
        binding.detailToolbar.apply {
            title = studyName
            setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        binding.tbCam.setOnCheckedChangeListener { button, isChecked ->
            if (isChecked) {
                button.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.cam_on, 0, 0, 0)
                button.backgroundTintList =
                    ContextCompat.getColorStateList(button.context, R.color.green_light)
            } else {
                button.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.cam_off, 0, 0, 0)
                button.backgroundTintList =
                    ContextCompat.getColorStateList(button.context, R.color.red_light)
            }
            webRtcClientConnection.localVideoTrack?.setEnabled(isChecked)
        }

        binding.tbMic.setOnCheckedChangeListener { button, isChecked ->
            if (isChecked) {
                button.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.mic_on, 0, 0, 0)
                button.backgroundTintList =
                    ContextCompat.getColorStateList(button.context, R.color.green_light)
            } else {
                button.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.mic_off, 0, 0, 0)
                button.backgroundTintList =
                    ContextCompat.getColorStateList(button.context, R.color.red_light)
            }
            webRtcClientConnection.localAudioTrack?.setEnabled(isChecked)
        }

        binding.ivSendChat.setOnClickListener {
            val message = binding.etChatInput.text.toString().trim()
            lifecycleScope.launch {
                val result = webRtcClientConnection.sendChatMessage(message)
                if (result) {
                    displayChatMessage(true, NICKNAME, message)
                }
                binding.etChatInput.setText("")
            }
        }

        binding.btnEndLive.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun displayChatMessage(isMe: Boolean, nickname: String, message: String) {
        lifecycleScope.launch {
            val chat = LiveChatMessage(isMe, nickname, message)
            Log.d(TAG, "displayChatMessage: $chat")
            liveChatMessages.add(chat)
            liveChatAdapter.notifyItemInserted(liveChatMessages.lastIndex)

            val layoutManager = binding.rvChat.layoutManager as LinearLayoutManager
            // 현재 마지막으로 보이는 아이템이 전체 아이템의 마지막 바로 위라면(즉, 사용자가 맨 아래에 있을 때)
            if (layoutManager.findLastVisibleItemPosition() >= liveChatMessages.lastIndex - 1) {
                binding.rvChat.smoothScrollToPosition(liveChatMessages.lastIndex)
            }
        }
    }

    private fun updatePeopleCount() {
        binding.peopleCountTextView.text = "${liveMembers.size}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        webRtcClientConnection.leaveRoom()
        webRtcClientConnection.close()
        _binding = null
    }


}
