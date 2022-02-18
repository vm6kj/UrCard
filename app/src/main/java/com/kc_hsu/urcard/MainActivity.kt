package com.kc_hsu.urcard

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.wear.tiles.manager.TileUiClient
import com.kc_hsu.urcard.databinding.ActivityMainBinding

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var tileUiClient: TileUiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tileUiClient = TileUiClient(
            context = this,
            component = ComponentName(this, CardTileService::class.java),
            parentView = binding.root
        )

        tileUiClient.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        tileUiClient.close()
    }
}