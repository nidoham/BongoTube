package bd.nidoham.bongo.ui.fragments.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import bd.nidoham.bongo.R;

public class ReelFragment extends Fragment {

    public ReelFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull final LayoutInflater inflater,
            @Nullable final ViewGroup container,
            @Nullable final Bundle savedInstanceState) {
        // Layout inflate করে root view তৈরি করা হচ্ছে
        final View view = inflater.inflate(R.layout.fragment_reels, container, false);

        return view;
    }
}
