package bd.nidoham.bongo.fragments.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import bd.nidoham.bongo.R;
import bd.nidoham.bongo.list.adapter.YoutubeAdapter;

public class YoutubeFragment extends Fragment implements YoutubeAdapter.OnItemClickListener {

    private static final String TAG = "YoutubeFragment";

    // UI Components
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private YoutubeAdapter adapter;
    private View loadingView;
    private View errorView;
    private Button retryButton;

    // Data loading thread
    private Thread loadingThread;

    // Current country for trending videos
    private ContentCountry currentCountry;

    public YoutubeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull final LayoutInflater inflater,
            @Nullable final ViewGroup container,
            @Nullable final Bundle savedInstanceState) {
        
        // Layout inflate করে root view তৈরি করা হচ্ছে
        final View view = inflater.inflate(R.layout.fragment_home_youtube, container, false);
        
        initializeViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        setupRetryButton();
        initializeCountry();
        
        // Load trending videos for current country
        loadTrendingVideos();
        
        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        loadingView = view.findViewById(R.id.loadingView);
        errorView = view.findViewById(R.id.errorView);
        retryButton = view.findViewById(R.id.retryButton);
        
        // Initialize adapter with context
        if (getContext() != null) {
            adapter = new YoutubeAdapter(getContext());
            adapter.setOnItemClickListener(this);
        }
    }

    private void setupRecyclerView() {
        if (recyclerView != null && adapter != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapter);
            recyclerView.setHasFixedSize(true);
            
            // Optional: Add item decoration for spacing
            // recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        }
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadTrendingVideos);
            swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorPrimaryVariant
            );
        }
    }

    private void setupRetryButton() {
        if (retryButton != null) {
            retryButton.setOnClickListener(v -> {
                Log.d(TAG, "Retry button clicked - loading trending videos");
                loadTrendingVideos();
            });
        }
    }

    private void initializeCountry() {
        try {
            // Get current country from NewPipe preferences
            currentCountry = NewPipe.getPreferredContentCountry();
            if (currentCountry != null) {
                Log.d(TAG, "Current country set to: " + currentCountry.getCountryCode());
            } else {
                Log.d(TAG, "No preferred country set, using default");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting preferred country", e);
            currentCountry = null;
        }
    }

    private void showLoading(boolean show) {
        if (getActivity() != null && !isDetached() && isAdded()) {
            getActivity().runOnUiThread(() -> {
                try {
                    if (loadingView != null) {
                        loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
                    }
                    if (recyclerView != null) {
                        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
                    }
                    if (errorView != null) {
                        errorView.setVisibility(View.GONE);
                    }
                    if (swipeRefreshLayout != null && !show) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating loading state", e);
                }
            });
        }
    }

    private void showError(String message) {
        if (getActivity() != null && !isDetached() && isAdded()) {
            getActivity().runOnUiThread(() -> {
                try {
                    if (errorView != null) {
                        errorView.setVisibility(View.VISIBLE);
                    }
                    if (loadingView != null) {
                        loadingView.setVisibility(View.GONE);
                    }
                    if (recyclerView != null) {
                        recyclerView.setVisibility(View.GONE);
                    }
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    showToastOnUiThread(message);
                } catch (Exception e) {
                    Log.e(TAG, "Error updating error state", e);
                }
            });
        }
    }

    private void showContent() {
        if (getActivity() != null && !isDetached() && isAdded()) {
            getActivity().runOnUiThread(() -> {
                try {
                    if (recyclerView != null) {
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                    if (loadingView != null) {
                        loadingView.setVisibility(View.GONE);
                    }
                    if (errorView != null) {
                        errorView.setVisibility(View.GONE);
                    }
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating content state", e);
                }
            });
        }
    }

    private void loadTrendingVideos() {
        // Cancel previous loading thread if running
        if (loadingThread != null && loadingThread.isAlive()) {
            loadingThread.interrupt();
        }

        showLoading(true);
        
        final int youtubeServiceId = 0;
        final String trendingKioskId = "Trending";

        loadingThread = new Thread(() -> {
            try {
                // Check if thread was interrupted
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }

                // Check if NewPipe Downloader is initialized
                if (NewPipe.getDownloader() == null) {
                    Log.e(TAG, "NewPipe Downloader is not initialized!");
                    showError("NewPipe Downloader is not initialized!");
                    return;
                }

                // Get YouTube service
                StreamingService service = NewPipe.getService(youtubeServiceId);
                if (service == null || service.getKioskList() == null) {
                    Log.e(TAG, "YouTube service or KioskList not available.");
                    showError("YouTube service not available");
                    return;
                }

                List<StreamInfoItem> trendingItems = new ArrayList<>();
                
                // Get current country for trending videos
                String countryCode = currentCountry != null ? currentCountry.getCountryCode() : "US";
                Log.d(TAG, "Loading trending videos for country: " + countryCode);
                
                // Get trending videos extractor for current country
                KioskExtractor extractor = service.getKioskList().getExtractorById(trendingKioskId, null);
                if (extractor != null) {
                    // Check if thread was interrupted before network call
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }

                    // Set content country if available
                    if (currentCountry != null) {
                        extractor.forceContentCountry(currentCountry);
                    }

                    extractor.fetchPage();
                    List<StreamInfoItem> items = extractor.getInitialPage().getItems();
                    
                    if (items != null && !items.isEmpty()) {
                        trendingItems.addAll(items);
                        Log.d(TAG, "Successfully fetched " + items.size() + " trending videos for " + countryCode);
                    } else {
                        Log.w(TAG, "No trending videos found for country: " + countryCode);
                    }
                } else {
                    Log.e(TAG, "Could not get KioskExtractor for trending videos");
                    showError("Could not access trending videos");
                    return;
                }

                // Check if thread was interrupted before UI update
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }

                // Update UI on the main thread
                final List<StreamInfoItem> finalItems = new ArrayList<>(trendingItems);
                if (getActivity() != null && !isDetached() && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            if (adapter == null) {
                                Log.e(TAG, "Adapter is null when trying to update content");
                                return;
                            }
                            
                            if (finalItems.isEmpty()) {
                                showError("No trending videos found for your country");
                            } else {
                                adapter.submitList(finalItems);
                                showContent();
                                Log.d(TAG, "Updated adapter with " + finalItems.size() + " trending videos");
                                
                                // Show success message
                                String countryName = currentCountry != null ? currentCountry.getCountryCode() : "Default";
                                showToastOnUiThread("Loaded trending videos for " + countryName);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating UI with trending video data", e);
                            showError("Error displaying trending videos");
                        }
                    });
                }

            } catch (IOException e) {
                Log.e(TAG, "Network error loading trending videos", e);
                showError("Network error: Check your internet connection");
            } catch (ExtractionException e) {
                Log.e(TAG, "Extraction error loading trending videos", e);
                showError("Error loading trending videos from YouTube");
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error occurred while loading trending videos", e);
                showError("An unexpected error occurred");
            }
        });
        
        loadingThread.start();
    }

    private void showToastOnUiThread(String message) {
        if (getActivity() != null && !isDetached() && isAdded()) {
            getActivity().runOnUiThread(() -> {
                try {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error showing toast", e);
                }
            });
        }
    }

    @Override
    public void onItemClick(StreamInfoItem item, int position) {
        // Handle trending video item click
        Log.d(TAG, "Trending video clicked: " + item.getName() + " at position: " + position);
        
        // Show video info
        if (getContext() != null) {
            String message = "Playing trending video: " + item.getName();
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
        
        // TODO: Implement actual video playback logic
        // For example:
        // Intent intent = new Intent(getActivity(), VideoPlayerActivity.class);
        // intent.putExtra("video_url", item.getUrl());
        // intent.putExtra("video_title", item.getName());
        // intent.putExtra("video_type", "trending");
        // intent.putExtra("country_code", currentCountry != null ? currentCountry.getCountryCode() : "default");
        // startActivity(intent);
    }

    public void refreshTrendingVideos() {
        Log.d(TAG, "Manually refreshing trending videos");
        loadTrendingVideos();
    }

    public String getCurrentCountryCode() {
        return currentCountry != null ? currentCountry.getCountryCode() : "Default";
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop loading when fragment is paused
        if (loadingThread != null && loadingThread.isAlive()) {
            loadingThread.interrupt();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Cancel any running threads
        if (loadingThread != null && loadingThread.isAlive()) {
            loadingThread.interrupt();
        }
        
        // Clean up references to prevent memory leaks
        if (adapter != null) {
            adapter.setOnItemClickListener(null);
            adapter = null;
        }
        
        recyclerView = null;
        swipeRefreshLayout = null;
        loadingView = null;
        errorView = null;
        retryButton = null;
        loadingThread = null;
        currentCountry = null;
        
        Log.d(TAG, "YoutubeFragment view destroyed and cleaned up");
    }
}