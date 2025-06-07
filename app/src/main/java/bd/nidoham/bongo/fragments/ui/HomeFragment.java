package bd.nidoham.bongo.ui.fragments.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import bd.nidoham.bongo.ui.fragments.adapter.HomePagerAdapter;
import com.google.android.material.tabs.TabLayoutMediator;

import bd.nidoham.bongo.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomePagerAdapter adapter;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ✅ Set up ViewPager2 with MainPagerAdapter
        adapter = new HomePagerAdapter(requireActivity());
        binding.vidmateContentPager.setAdapter(adapter);
        
        // Disable swipe gesture
        binding.vidmateContentPager.setUserInputEnabled(false);

        // ✅ Attach TabLayout with ViewPager2
        new TabLayoutMediator(
                binding.vidmateTabLayout,
                binding.vidmateContentPager,
                (tab, position) -> tab.setText(adapter.getTitle(position))
        ).attach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leaks
    }
}