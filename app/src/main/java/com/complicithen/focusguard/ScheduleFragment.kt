package com.complicithen.focusguard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.complicithen.focusguard.databinding.FragmentScheduleBinding
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class ScheduleFragment : Fragment() {
    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!
    private lateinit var scheduleManager: ScheduleManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scheduleManager = ScheduleManager(requireContext())

        // Focus schedule switch
        binding.switchFocusSchedule.isChecked = scheduleManager.focusEnabled
        binding.switchFocusSchedule.setOnCheckedChangeListener { _, checked ->
            scheduleManager.focusEnabled = checked
            scheduleManager.scheduleAll()
        }

        // Bedtime switch
        binding.switchBedtime.isChecked = scheduleManager.bedtimeEnabled
        binding.switchBedtime.setOnCheckedChangeListener { _, checked ->
            scheduleManager.bedtimeEnabled = checked
            scheduleManager.scheduleAll()
        }

        // Focus schedule time pickers
        binding.btnPickFocusStart.setOnClickListener {
            showTimePicker(scheduleManager.focusStartHour, scheduleManager.focusStartMin) { h, m ->
                scheduleManager.focusStartHour = h
                scheduleManager.focusStartMin = m
                scheduleManager.scheduleAll()
                updateTimeLabels()
            }
        }
        binding.btnPickFocusEnd.setOnClickListener {
            showTimePicker(scheduleManager.focusEndHour, scheduleManager.focusEndMin) { h, m ->
                scheduleManager.focusEndHour = h
                scheduleManager.focusEndMin = m
                scheduleManager.scheduleAll()
                updateTimeLabels()
            }
        }

        // Bedtime time pickers
        binding.btnPickBedtimeStart.setOnClickListener {
            showTimePicker(scheduleManager.bedtimeStartHour, scheduleManager.bedtimeStartMin) { h, m ->
                scheduleManager.bedtimeStartHour = h
                scheduleManager.bedtimeStartMin = m
                scheduleManager.scheduleAll()
                updateTimeLabels()
            }
        }
        binding.btnPickBedtimeEnd.setOnClickListener {
            showTimePicker(scheduleManager.bedtimeEndHour, scheduleManager.bedtimeEndMin) { h, m ->
                scheduleManager.bedtimeEndHour = h
                scheduleManager.bedtimeEndMin = m
                scheduleManager.scheduleAll()
                updateTimeLabels()
            }
        }

        updateTimeLabels()
    }

    private fun showTimePicker(hour: Int, minute: Int, onSet: (Int, Int) -> Unit) {
        MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(hour)
            .setMinute(minute)
            .build()
            .apply {
                addOnPositiveButtonClickListener { onSet(this.hour, this.minute) }
                show(parentFragmentManager, "time_picker")
            }
    }

    private fun updateTimeLabels() {
        binding.textFocusStart.text = fmt(scheduleManager.focusStartHour, scheduleManager.focusStartMin)
        binding.textFocusEnd.text = fmt(scheduleManager.focusEndHour, scheduleManager.focusEndMin)
        binding.textBedtimeStart.text = fmt(scheduleManager.bedtimeStartHour, scheduleManager.bedtimeStartMin)
        binding.textBedtimeEnd.text = fmt(scheduleManager.bedtimeEndHour, scheduleManager.bedtimeEndMin)
    }

    private fun fmt(h: Int, m: Int) = String.format("%02d:%02d", h, m)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
