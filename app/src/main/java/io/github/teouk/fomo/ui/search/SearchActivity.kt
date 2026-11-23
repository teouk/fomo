package io.github.teouk.fomo.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.teouk.fomo.databinding.ActivitySearchBinding

/** Search & Filter screen. Opened from the search bar on Discover. */
class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // finish() closes this screen and returns to whichever screen opened it.
        binding.toolbarSearch.setNavigationOnClickListener { finish() }
    }

    companion object {
        private const val EXTRA_SCAN_POSTER = "io.github.teouk.fomo.extra.SCAN_POSTER"

        /**
         * Builds the Intent that opens this screen, so callers never construct it by hand.
         * @param scanPoster open the camera straight away (Poster Scanner).
         */
        fun newIntent(context: Context, scanPoster: Boolean = false): Intent =
            Intent(context, SearchActivity::class.java).putExtra(EXTRA_SCAN_POSTER, scanPoster)
    }
}
