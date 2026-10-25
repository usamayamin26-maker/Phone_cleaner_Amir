package com.phone.cleaner.presentation.onboarding

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.doOnLayout
import com.phone.cleaner.databinding.ActivitySplashBinding
import com.phone.cleaner.domain.view_models.SplashDestination
import com.phone.cleaner.domain.view_models.SplashUiState
import com.phone.cleaner.domain.view_models.SplashViewModel
import com.phone.cleaner.presentation.MainActivity
import com.phone.cleaner.presentation.base.BaseActivity
import com.phone.cleaner.utils.applyNavigationBarBottomMargin
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * The launcher. The system splash from core-splashscreen covers the cold start with the same logo,
 * then this screen shows the design's splash for about two seconds while the loading bar fills,
 * and moves on to the Language screen on the first run or to Home after that.
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    override val bindingInflater: (LayoutInflater) -> ActivitySplashBinding =
        ActivitySplashBinding::inflate

    /** The background runs behind both bars; the loading line keeps clear of the navigation bar. */
    override val applyDefaultInsets: Boolean = false

    private val viewModel: SplashViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
    }

    override fun setupViews() = with(binding) {
        loadingText.applyNavigationBarBottomMargin()
        fillProgressBar()
    }

    override fun observeState() {
        viewModel.state.collectState(::render)
    }

    /** The bar fills from the start edge over the hold, so it is full as the splash leaves. */
    private fun fillProgressBar() = binding.progressFill.doOnLayout { fill ->
        fill.pivotX = if (fill.layoutDirection == View.LAYOUT_DIRECTION_RTL) fill.width.toFloat() else 0f
        fill.scaleX = 0f
        fill.animate()
            .scaleX(1f)
            .setDuration(SplashViewModel.HOLD_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun render(state: SplashUiState) {
        val destination = state.destination ?: return
        if (isFinishing) return
        val next = when (destination) {
            SplashDestination.LANGUAGE -> LanguageActivity::class.java
            SplashDestination.HOME -> MainActivity::class.java
        }
        startActivity(Intent(this, next))
        finish()
    }
}
