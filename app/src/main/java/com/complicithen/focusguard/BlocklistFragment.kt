package com.complicithen.focusguard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.complicithen.focusguard.databinding.FragmentBlocklistBinding

class BlocklistFragment : Fragment() {
    private var _binding: FragmentBlocklistBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBlocklistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCopyCloudflare.setOnClickListener {
            copyToClipboard("family.cloudflare-dns.com")
        }
        binding.btnCopyAdguard.setOnClickListener {
            copyToClipboard("family.adguard-dns.com")
        }
        binding.btnCopyCleanbrowsing.setOnClickListener {
            copyToClipboard("family-filter-dns.cleanbrowsing.org")
        }

        binding.btnOpenDnsSettings.setOnClickListener {
            openPrivateDnsSettings()
        }
    }

    private fun copyToClipboard(hostname: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("DNS hostname", hostname))
        Toast.makeText(requireContext(), "Copied: $hostname", Toast.LENGTH_SHORT).show()
    }

    private fun openPrivateDnsSettings() {
        // Try the direct Private DNS settings screen, fall back to general network settings
        val intents = listOf(
            Intent("android.settings.PRIVATE_DNS_SETTINGS"),
            Intent(Settings.ACTION_WIRELESS_SETTINGS),
            Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS)
        )
        for (intent in intents) {
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
                return
            }
        }
        Toast.makeText(requireContext(), "Open Settings → Network → Private DNS", Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
