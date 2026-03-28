package com.complicithen.focusguard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.complicithen.focusguard.databinding.FragmentBlocklistBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView

private const val DNS_CLOUDFLARE = "family.cloudflare-dns.com"
private const val DNS_ADGUARD = "family.adguard-dns.com"
private const val DNS_CLEANBROWSING = "family-filter-dns.cleanbrowsing.org"

class BlocklistFragment : Fragment() {
    private var _binding: FragmentBlocklistBinding? = null
    private val binding get() = _binding!!
    private lateinit var appFilterManager: AppFilterManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBlocklistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appFilterManager = AppFilterManager(requireContext())
        setupAppFilter()
        setupDns()
    }

    // ─── App filter ───────────────────────────────────────────────────────────

    private fun setupAppFilter() {
        binding.switchSelectiveFilter.isChecked = appFilterManager.selectiveMode
        updateFilterModeUi(appFilterManager.selectiveMode)

        binding.switchSelectiveFilter.setOnCheckedChangeListener { _, checked ->
            appFilterManager.selectiveMode = checked
            updateFilterModeUi(checked)
        }

        binding.btnAddApp.setOnClickListener { showAppPickerDialog() }
    }

    private fun updateFilterModeUi(selective: Boolean) {
        binding.layoutSelectedApps.isVisible = selective
        binding.textFilterModeDesc.text =
            if (selective) "Only selected apps will be filtered"
            else "Currently: all apps are filtered"
    }

    private fun refreshSelectedApps() {
        binding.containerApps.removeAllViews()
        val selected = appFilterManager.getFilteredApps()
        binding.textNoAppsSelected.isVisible = selected.isEmpty()

        val pm = requireContext().packageManager
        selected.sorted().forEach { pkg ->
            val label = try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } catch (_: Exception) { pkg }
            binding.containerApps.addView(buildAppRow(pkg, label))
        }
    }

    private fun buildAppRow(packageName: String, label: String): View {
        val ctx = requireContext()
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)

            addView(MaterialTextView(ctx).apply {
                text = label
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            addView(MaterialButton(ctx, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
                text = "Remove"
                setTextColor(ctx.getColor(R.color.red_700))
                textSize = 13f
                setOnClickListener {
                    appFilterManager.remove(packageName)
                    refreshSelectedApps()
                }
            })
        }
    }

    private fun showAppPickerDialog() {
        val pm = requireContext().packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }

        val labels = apps.map { pm.getApplicationLabel(it).toString() }.toTypedArray()
        val filtered = appFilterManager.getFilteredApps()
        val checked = BooleanArray(apps.size) { filtered.contains(apps[it].packageName) }

        AlertDialog.Builder(requireContext())
            .setTitle("Select apps to filter")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                if (isChecked) appFilterManager.add(apps[which].packageName)
                else appFilterManager.remove(apps[which].packageName)
            }
            .setPositiveButton("Done") { _, _ -> refreshSelectedApps() }
            .show()
    }

    // ─── DNS ──────────────────────────────────────────────────────────────────

    private fun setupDns() {
        binding.btnCopyCloudflare.setOnClickListener { copyToClipboard(DNS_CLOUDFLARE) }
        binding.btnCopyAdguard.setOnClickListener { copyToClipboard(DNS_ADGUARD) }
        binding.btnCopyCleanbrowsing.setOnClickListener { copyToClipboard(DNS_CLEANBROWSING) }

        binding.btnApplyCloudflare.setOnClickListener { applyDns(DNS_CLOUDFLARE) }
        binding.btnApplyAdguard.setOnClickListener { applyDns(DNS_ADGUARD) }
        binding.btnApplyCleanbrowsing.setOnClickListener { applyDns(DNS_CLEANBROWSING) }

        binding.btnOpenDnsSettings.setOnClickListener { openPrivateDnsSettings() }
    }

    private fun updateDnsStatus() {
        val mode = Settings.Global.getString(requireContext().contentResolver, "private_dns_mode")
        val host = Settings.Global.getString(requireContext().contentResolver, "private_dns_specifier") ?: ""

        val statusText = when {
            mode == "hostname" && host.isNotEmpty() ->
                "Active: $host"
            mode == "opportunistic" ->
                "Private DNS: opportunistic (no content filter)"
            mode == "off" || mode == null ->
                "Private DNS: off — tap Apply to enable"
            else ->
                "Private DNS: $mode"
        }

        val isActive = mode == "hostname" && listOf(DNS_CLOUDFLARE, DNS_ADGUARD, DNS_CLEANBROWSING)
            .any { host.equals(it, ignoreCase = true) }

        binding.textDnsStatus.text = statusText
        val (bgColor, textColor, strokeColor) = if (isActive)
            Triple(R.color.green_100, R.color.green_800, R.color.green_700)
        else
            Triple(R.color.amber_50, R.color.amber_800, R.color.amber_800)

        binding.cardDnsStatus.setCardBackgroundColor(requireContext().getColor(bgColor))
        binding.textDnsStatus.setTextColor(requireContext().getColor(textColor))
        binding.cardDnsStatus.strokeColor = requireContext().getColor(strokeColor)
    }

    /** Try to apply DNS programmatically. Requires WRITE_SECURE_SETTINGS (grantable via adb). */
    private fun applyDns(hostname: String) {
        try {
            Settings.Global.putString(requireContext().contentResolver, "private_dns_mode", "hostname")
            Settings.Global.putString(requireContext().contentResolver, "private_dns_specifier", hostname)
            updateDnsStatus()
            Toast.makeText(requireContext(), "DNS set to $hostname", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            // Need WRITE_SECURE_SETTINGS — show adb grant instructions
            AlertDialog.Builder(requireContext())
                .setTitle("Permission needed")
                .setMessage(
                    "To auto-apply DNS, run this command once via ADB (USB or wireless):\n\n" +
                    "adb shell pm grant com.complicithen.focusguard.debug " +
                    "android.permission.WRITE_SECURE_SETTINGS\n\n" +
                    "Then tap Apply again.\n\n" +
                    "Alternatively use the Copy button and set DNS manually."
                )
                .setPositiveButton("Copy ADB command") { _, _ ->
                    copyToClipboard(
                        "adb shell pm grant com.complicithen.focusguard.debug " +
                        "android.permission.WRITE_SECURE_SETTINGS"
                    )
                }
                .setNegativeButton("Manual setup") { _, _ -> openPrivateDnsSettings() }
                .show()
        }
    }

    private fun copyToClipboard(text: String) {
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("dns", text))
        Toast.makeText(requireContext(), "Copied", Toast.LENGTH_SHORT).show()
    }

    private fun openPrivateDnsSettings() {
        listOf(
            Intent("android.settings.PRIVATE_DNS_SETTINGS"),
            Intent(Settings.ACTION_WIRELESS_SETTINGS)
        ).firstOrNull { it.resolveActivity(requireContext().packageManager) != null }
            ?.let { startActivity(it) }
            ?: Toast.makeText(requireContext(), "Open Settings → Network → Private DNS", Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        refreshSelectedApps()
        updateDnsStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
