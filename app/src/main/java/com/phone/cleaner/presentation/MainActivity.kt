package com.phone.cleaner.presentation

import android.view.LayoutInflater
import com.phone.cleaner.databinding.ActivityMainBinding
import com.phone.cleaner.presentation.base.BaseActivity

/**
 * The app's main screen. It will host the Home screen from the design; every feature (Device Info,
 * Battery Info, WA Cleaner, ...) opens on top of it as its own activity.
 */
class MainActivity : BaseActivity<ActivityMainBinding>() {

    override val bindingInflater: (LayoutInflater) -> ActivityMainBinding =
        ActivityMainBinding::inflate
}
