package bd.nidoham.bongo.ui.fragments.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import bd.nidoham.bongo.fragments.ui.home.SearchFragment;
import bd.nidoham.bongo.fragments.ui.home.SubscriptionFragment;
import bd.nidoham.bongo.fragments.ui.home.YoutubeFragment;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;


public class HomePagerAdapter extends FragmentStateAdapter {

    public enum Page {
        SEARCH(0, "Search", SearchFragment::new),
        YOUTUBE(1, "YouTube", YoutubeFragment::new),
        SUBSCRIPTION(2, "Subscription", SubscriptionFragment::new);

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
                case 0 -> SEARCH;
                case 1 -> YOUTUBE;
                case 2 -> SUBSCRIPTION;
                default -> throw new IllegalArgumentException("Invalid position: " + pos);
            };
        }
    }

    private final Map<Integer, Fragment> fragmentCache = new HashMap<>();

    public HomePagerAdapter(@NonNull FragmentActivity activity) {
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