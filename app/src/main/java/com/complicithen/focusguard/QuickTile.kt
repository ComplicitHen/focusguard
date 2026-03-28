package com.complicithen.focusguard

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class QuickTile : TileService() {

    override fun onTileAdded() = refreshTile()
    override fun onStartListening() = refreshTile()

    override fun onClick() {
        val fm = FocusManager(this)
        if (fm.isEnabled()) fm.disable() else fm.enable()
        refreshTile()
    }

    private fun refreshTile() {
        val active = FocusManager(this).isEnabled()
        qsTile?.apply {
            state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "FocusGuard"
            contentDescription = if (active) "Focus mode on" else "Focus mode off"
            updateTile()
        }
    }
}
