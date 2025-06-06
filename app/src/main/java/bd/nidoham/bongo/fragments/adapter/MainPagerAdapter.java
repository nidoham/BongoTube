package bd.nidoham.bongo.ui.fragments.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import bd.nidoham.bongo.ui.fragments.ui.HomeFragment;
import bd.nidoham.bongo.ui.fragments.ui.ReelFragment;
import bd.nidoham.bongo.ui.fragments.ui.AccountFragment;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Supplier;

public class MainPagerAdapter extends FragmentStateAdapter {

    public enum Page {
        HOME(0, "Home", HomeFragment::new),
        REEL(1, "Reel", ReelFragment::new),
        ACCOUNT(2, "Me", AccountFragment::new);

        public final int position;
        public final String title;
        public final Supplier<Fragment> supplier;

        Page(int position, String title, Supplier<Fragment> supplier) {
            this.position = position;
            this.title = title;
            this.supplier = supplier;
        }

        public static Page fromPosition(int pos) {
            return switch (pos) {
                case 0 -> HOME;
                case 1 -> REEL;
                case 2 -> ACCOUNT;
                default -> throw new IllegalArgumentException("Invalid position: " + pos);
            };
        }
    }

    private final Map<Integer, Fragment> fragmentCache = new HashMap<>();

    public MainPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (fragmentCache.containsKey(position)) {
            return fragmentCache.get(position);
        }
        Page page = Page.fromPosition(position);
        Fragment fragment = page.supplier.get();
        fragmentCache.put(position, fragment);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return Page.values().length;
    }

    public Fragment getFragmentByPosition(int position) {
        return fragmentCache.get(position);
    }

    public String getTitle(int position) {
        return Page.fromPosition(position).title;
    }
}