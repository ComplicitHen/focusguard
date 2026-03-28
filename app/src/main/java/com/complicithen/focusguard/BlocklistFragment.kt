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
        binding.btnCopyCloudflare.setOnClickListener { copyToClipboard("family.cloudflare-dns.com") }
        binding.btnCopyAdguard.setOnClickListener { copyToClipboard("family.adguard-dns.com") }
        binding.btnCopyCleanbrowsing.setOnClickListener { copyToClipboard("family-filter-dns.cleanbrowsing.org") }
        binding.btnOpenDnsSettings.setOnClickListener { openPrivateDnsSettings() }
    }

    private fun copyToClipboard(hostname: String) {
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("DNS hostname", hostname))
        Toast.makeText(requireContext(), "Copied: $hostname", Toast.LENGTH_SHORT).show()
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
