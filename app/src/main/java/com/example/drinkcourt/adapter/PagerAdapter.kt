package com.example.drinkcourt.adapter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.drinkcourt.menu_product.MenuFragment

class PagerAdapter(fragment: FragmentActivity, sizeI: Int): FragmentStateAdapter(fragment) {

    private var pos = sizeI

    override fun createFragment(position: Int): Fragment {
        val fragment = MenuFragment()
        fragment.arguments = Bundle().apply {
            putInt(MenuFragment.ARG_SECTION_NUMBER, position + 1)
        }
        return fragment
    }

    override fun getItemCount(): Int {
        return pos
    }
}