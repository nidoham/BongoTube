package bd.nidoham.bongo;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import bd.nidoham.bongo.databinding.ActivityMainBinding;
import bd.nidoham.bongo.ui.fragments.adapter.MainPagerAdapter;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private MainPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupViewPager();
        setupBottomNavigation();
    }

    private void setupViewPager() {
        pagerAdapter = new MainPagerAdapter(this);
        binding.vidmateContentPager.setAdapter(pagerAdapter);

        // Disable swipe gesture
        binding.vidmateContentPager.setUserInputEnabled(false);

        binding.vidmateContentPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                syncBottomNav(position);
            }
        });
    }

    private void setupBottomNavigation() {
        binding.vidmateBottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            int pageIndex = 0;

            if (itemId == R.id.nav_home) {
                pageIndex = 0;
            } else if (itemId == R.id.nav_reel) {
                pageIndex = 1;
            } else if (itemId == R.id.nav_account) {
                pageIndex = 2;
            }

            binding.vidmateContentPager.setCurrentItem(pageIndex, false);
            return true;
        });
    }

    private void syncBottomNav(int position) {
        int itemId = R.id.nav_home;

        if (position == 0) {
            itemId = R.id.nav_home;
        } else if (position == 1) {
            itemId = R.id.nav_reel;
        } else if (position == 2) {
            itemId = R.id.nav_account;
        }

        binding.vidmateBottomNav.setSelectedItemId(itemId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.binding = null;
    }
}