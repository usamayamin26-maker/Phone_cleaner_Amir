package com.phone.cleaner.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.phone.cleaner.databinding.ItemOnboardingPageBinding
import com.phone.cleaner.domain.entities.OnboardingPage

/**
 * The content of the four onboarding pages: title, subtitle and phone image. Skip, the indicator
 * and the action button belong to the activity, so they do not move with a swipe.
 */
class OnboardingAdapter : RecyclerView.Adapter<OnboardingAdapter.PageViewHolder>() {

    private val pages = OnboardingPage.entries

    /**
     * Height at the bottom of every page that is kept free for the activity's controls. The image
     * shrinks into what is left rather than running under them.
     */
    var bottomReserve: Int = 0
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    override fun getItemCount(): Int = pages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder =
        PageViewHolder(ItemOnboardingPageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) = holder.bind(pages[position])

    inner class PageViewHolder(
        private val binding: ItemOnboardingPageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(page: OnboardingPage) = with(binding) {
            pageTitle.setText(page.title)
            pageSubtitle.setText(page.subtitle)
            pageImage.setImageResource(page.image)
            root.updatePadding(bottom = bottomReserve)
        }
    }
}
