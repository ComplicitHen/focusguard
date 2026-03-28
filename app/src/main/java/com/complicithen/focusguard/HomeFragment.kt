package com.complicithen.focusguard

import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.complicithen.focusguard.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var focusManager: FocusManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        focusManager = FocusManager(requireContext())

        binding.switchFocusMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) focusManager.enable() else focusManager.disable()
            updateStatus()
            focusManager.syncStatusNotification()
        }

        binding.btnGrantNotifListener.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        binding.btnGrantCallScreening.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager =
                    requireContext().getSystemService(RoleManager::class.java)
                val intent =
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                callScreeningLauncher.launch(intent)
            }
        }

        binding.btnGrantNotifications.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val callScreeningLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { updateStatus() }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { updateStatus() }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val hasCallScreening = hasCallScreeningRole()
        val hasNotifListener = isNotificationListenerEnabled()
        val hasNotifPermission = hasNotificationPermission()
        val allGranted = hasCallScreening && hasNotifListener && hasNotifPermission

        // Notification permission row
        binding.statusNotifications.text =
            if (hasNotifPermission) "Granted" else "Required to show hourly reminders"
        binding.statusNotifications.setTextColor(statusColor(hasNotifPermission))
        binding.btnGrantNotifications.visibility =
            if (hasNotifPermission) View.GONE else View.VISIBLE

        // Call screening row
        binding.statusCallScreening.text =
            if (hasCallScreening) "Granted" else "Required to block non-whitelisted calls"
        binding.statusCallScreening.setTextColor(statusColor(hasCallScreening))
        binding.btnGrantCallScreening.visibility =
            if (hasCallScreening) View.GONE else View.VISIBLE

        // Notification listener row
        binding.statusNotifListener.text =
            if (hasNotifListener) "Granted" else "Required to filter messages from strangers"
        binding.statusNotifListener.setTextColor(statusColor(hasNotifListener))
        binding.btnGrantNotifListener.visibility =
            if (hasNotifListener) View.GONE else View.VISIBLE

        // Focus mode switch
        binding.switchFocusMode.isChecked = focusManager.isEnabled()
        binding.switchFocusMode.isEnabled = allGranted

        binding.textPermissionsHint.visibility = if (allGranted) View.GONE else View.VISIBLE

        // Status card
        val isEnabled = focusManager.isEnabled()
        binding.cardStatus.setCardBackgroundColor(
            requireContext().getColor(
                if (isEnabled) R.color.green_100 else R.color.grey_100
            )
        )
        binding.textFocusStatus.text =
            if (isEnabled) "Focus mode is ON" else "Focus mode is OFF"
        binding.textFocusDetail.text = if (isEnabled)
            "All notifications are held and released together at the top of each hour. Whitelisted numbers get through immediately."
        else
            "Enable to hold all notifications until the next complete hour. Add trusted numbers to the whitelist so they always get through."
    }

    private fun statusColor(granted: Boolean): Int {
        val colorRes = if (granted) R.color.green_700 else R.color.red_700
        return requireContext().getColor(colorRes)
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            requireContext().contentResolver,
            "enabled_notification_listeners"
        )
        return flat?.contains(requireContext().packageName) == true
    }

    private fun hasCallScreeningRole(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = requireContext().getSystemService(RoleManager::class.java)
            return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        }
        return false
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return requireContext().checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        return true // Below Android 13, no runtime permission needed
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
