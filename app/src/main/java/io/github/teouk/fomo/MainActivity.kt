package io.github.teouk.fomo

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import io.github.teouk.fomo.databinding.ActivityMainBinding
import io.github.teouk.fomo.ui.auth.SignInActivity
import io.github.teouk.fomo.ui.discover.DiscoverFragment
import io.github.teouk.fomo.ui.map.MapFragment
import io.github.teouk.fomo.ui.plans.PlansFragment
import io.github.teouk.fomo.ui.profile.ProfileFragment

/** Hosts the four bottom-navigation tabs. Each tab is a Fragment shown in fragment_container. */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app needs an account (CW2 spec). Signed-out users go to the sign-in screen.
        if ((application as FomoApplication).container.authRepository.currentAccount == null) {
            startActivity(SignInActivity.newIntent(this))
            finish()
            return
        }
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // savedInstanceState is null only on a fresh start. After a rotation Android
        // restores the fragment that was showing, so adding one again would stack two.
        if (savedInstanceState == null) {
            showTab(DiscoverFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_discover -> DiscoverFragment()
                R.id.nav_map -> MapFragment()
                R.id.nav_plans -> PlansFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> return@setOnItemSelectedListener false
            }
            showTab(fragment)
            true
        }
        // Tapping the tab that is already selected should not rebuild its screen.
        binding.bottomNav.setOnItemReselectedListener { }
    }

    private fun showTab(fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, fragment)
        }
    }
}
