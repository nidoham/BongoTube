package bd.nidoham.bongo.fragments.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
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

import bd.nidoham.bongo.cache.UserSessionManager;
import bd.nidoham.bongo.cache.PersonalizedFeedExtractor;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import bd.nidoham.bongo.R;
import bd.nidoham.bongo.list.adapter.YoutubeAdapter;

public class YoutubeFragment extends Fragment implements YoutubeAdapter.OnItemClickListener {

    private static final String TAG = "YoutubeFragment";
    
    // SharedPreferences constants
    private static final String PREFS_NAME = "BongoPreferences";
    private static final String KEY_COUNTRY_CODE = "country_code";
    private static final String DEFAULT_COUNTRY_CODE = "BD";

    private static final int MIN_INITIAL_VIDEOS = 40;

    // UI Components
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private YoutubeAdapter adapter;
    private View loadingView;
    private View errorView;
    private Button retryButton;

    private Thread loadingThread;
    private ContentCountry currentCountry;
    private SharedPreferences sharedPreferences;
    
    // Pagination state (For Trending Feed)
    private Page nextPage;
    private boolean hasMorePages = true;
    private AtomicBoolean isLoadingMore = new AtomicBoolean(false);
    private List<StreamInfoItem> currentItems = new ArrayList<>();
    private KioskExtractor currentExtractor;
    
    // User Session
    private UserSessionManager sessionManager;

    public YoutubeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_home_youtube, container, false);
        
        // Session Manager এবং অন্যান্য ভিউ ইনিশিয়ালাইজ করা
        if (getContext() != null) {
            sessionManager = new UserSessionManager(getContext());
        }
        
        initializeSharedPreferences();
        initializeViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        setupRetryButton();
        initializeCountry();
        
        // ব্যবহারকারীর লগইন স্ট্যাটাস অনুযায়ী ফিড লোড করা
        loadInitialFeed();
        
        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        loadingView = view.findViewById(R.id.loadingView);
        errorView = view.findViewById(R.id.errorView);
        retryButton = view.findViewById(R.id.retryButton);
        
        if (getContext() != null) {
            adapter = new YoutubeAdapter(getContext());
            adapter.setOnItemClickListener(this);
        }
    }

    private void setupRecyclerView() {
        if (recyclerView != null && adapter != null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(adapter);
            
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    // পার্সোনালাইজড ফিডের জন্য আপাতত কোনো পেজিনেশন নেই
                    if (sessionManager.isLoggedIn()) return;
                    
                    if (dy > 0) {
                        int visibleItemCount = layoutManager.getChildCount();
                        int totalItemCount = layoutManager.getItemCount();
                        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                        
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 10 && hasMorePages &&
                            !isLoadingMore.get() && !swipeRefreshLayout.isRefreshing()) {
                            loadMoreTrendingVideos();
                        }
                    }
                }
            });
        }
    }

    // Main method to decide which feed to load
    private void loadInitialFeed() {
        if (sessionManager.isLoggedIn()) {
            Log.d(TAG, "User is logged in. Loading personalized feed.");
            swipeRefreshLayout.setRefreshing(true); // ম্যানুয়াল রিফ্রেশের জন্য
            loadPersonalizedFeed();
        } else {
            Log.d(TAG, "User is not logged in. Loading public trending feed.");
            loadPublicTrendingFeed();
        }
    }

    // Method to load personalized feed for logged-in users
    private void loadPersonalizedFeed() {
        if (loadingThread != null && loadingThread.isAlive()) {
            loadingThread.interrupt();
        }
        showLoading(true);

        loadingThread = new Thread(() -> {
            try {
                String cookies = sessionManager.getSessionCookies();
                if (cookies == null) {
                    showError("Login session expired. Please login again.");
                    return;
                }

                PersonalizedFeedExtractor extractor = new PersonalizedFeedExtractor(cookies);
                List<StreamInfoItem> items = extractor.fetchInitialPage();
                
                // এখানে আর কোনো পেজিনেশন নেই, তাই hasMorePages = false
                hasMorePages = false;

                if (getActivity() != null && !isDetached() && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (items.isEmpty()) {
                            showError("Could not load your personalized feed.");
                        } else {
                            currentItems.clear();
                            currentItems.addAll(items);
                            adapter.submitList(new ArrayList<>(currentItems));
                            showContent();
                            showToastOnUiThread("Showing your personalized feed");
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load personalized feed", e);
                showError("Failed to load your feed. Try logging out and in again.");
            }
        });
        loadingThread.start();
    }

    // Method to load public trending videos (Old logic)
    private void loadPublicTrendingFeed() {
        if (loadingThread != null && loadingThread.isAlive()) {
            loadingThread.interrupt();
        }
        showLoading(true);

        loadingThread = new Thread(() -> {
            try {
                if (NewPipe.getDownloader() == null) {
                    showError("Downloader not initialized!");
                    return;
                }
                StreamingService service = NewPipe.getService(0);
                currentExtractor = service.getKioskList().getExtractorById("Trending", null);
                
                if (currentCountry != null) {
                    currentExtractor.forceContentCountry(currentCountry);
                }

                currentExtractor.fetchPage();
                ListExtractor.InfoItemsPage<StreamInfoItem> currentPage = currentExtractor.getInitialPage();
                List<StreamInfoItem> initialItems = new ArrayList<>(currentPage.getItems());
                Page pageCursor = currentPage.getNextPage();

                while (pageCursor != null && initialItems.size() < MIN_INITIAL_VIDEOS) {
                    currentPage = currentExtractor.getPage(pageCursor);
                    initialItems.addAll(currentPage.getItems());
                    pageCursor = currentPage.getNextPage();
                }
                
                nextPage = pageCursor;
                hasMorePages = nextPage != null;

                if (getActivity() != null && !isDetached() && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        currentItems.clear();
                        currentItems.addAll(initialItems);
                        adapter.submitList(new ArrayList<>(currentItems));
                        showContent();
                        showToastOnUiThread("Showing trending for " + (currentCountry != null ? currentCountry.getCountryCode() : "default"));
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to load trending feed", e);
                showError("Could not load trending videos.");
            }
        });
        loadingThread.start();
    }
    
    // Pagination for TRENDING videos ONLY
    private void loadMoreTrendingVideos() {
        if (!hasMorePages || nextPage == null || isLoadingMore.get()) return;

        if (!isLoadingMore.compareAndSet(false, true)) return;
        
        showToastOnUiThread("Loading more...");
        
        new Thread(() -> {
            try {
                ListExtractor.InfoItemsPage<StreamInfoItem> nextItemsPage = currentExtractor.getPage(nextPage);
                if (nextItemsPage != null && !nextItemsPage.getItems().isEmpty()) {
                    List<StreamInfoItem> newItems = nextItemsPage.getItems();
                    nextPage = nextItemsPage.getNextPage();
                    hasMorePages = nextPage != null;
                    
                    if (getActivity() != null && !isDetached() && isAdded()) {
                        getActivity().runOnUiThread(() -> {
                            currentItems.addAll(newItems);
                            adapter.submitList(new ArrayList<>(currentItems));
                            isLoadingMore.set(false);
                        });
                    }
                } else {
                    hasMorePages = false;
                    isLoadingMore.set(false);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading more videos", e);
                hasMorePages = false;
                isLoadingMore.set(false);
            }
        }).start();
    }
    
    // --- Helper and Lifecycle Methods ---

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            // refresh listener এখন loadInitialFeed কল করবে
            swipeRefreshLayout.setOnRefreshListener(this::loadInitialFeed);
            swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorPrimaryVariant);
        }
    }
    
    public void refreshTrendingVideos() {
        Log.d(TAG, "Manually refreshing feed");
        loadInitialFeed();
    }

    private void setupRetryButton() {
        if (retryButton != null) {
            retryButton.setOnClickListener(v -> loadInitialFeed());
        }
    }
    
    private void showToastOnUiThread(String message) {
        if (getActivity() != null && !isDetached() && isAdded()) {
            getActivity().runOnUiThread(() -> {
                if(getContext() != null) Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    // Other methods like initializeSharedPreferences, initializeCountry, showLoading, showError, showContent, onItemClick etc.
    // এই মেথডগুলো অপরিবর্তিত থাকবে, তাই আমি সেগুলো এখানে আর রিপিট করছি না।
    // আপনার আগের কোড থেকে এগুলো কপি করে নিতে পারেন। নিচে প্রয়োজনীয় গুলো দিয়ে দিলাম।

    private void initializeSharedPreferences() {
        if (getContext() != null) {
            sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }

    private void initializeCountry() {
        try {
            String savedCountryCode = sharedPreferences.getString(KEY_COUNTRY_CODE, DEFAULT_COUNTRY_CODE);
            currentCountry = new ContentCountry(savedCountryCode);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing country", e);
        }
    }

    private void showLoading(boolean show) {
        if (getActivity() != null && !isDetached() && isAdded()) {
            getActivity().runOnUiThread(() -> {
                loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
                errorView.setVisibility(View.GONE);
                if (!show) swipeRefreshLayout.setRefreshing(false);
            });
        }
    }

    private void showError(String message) {
        if (getActivity() != null && !isDetached() && isAdded()) {
            getActivity().runOnUiThread(() -> {
                errorView.setVisibility(View.VISIBLE);
                loadingView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                showToastOnUiThread(message);
            });
        }
    }

    private void showContent() {
        if (getActivity() != null && !isDetached() && isAdded()) {
            getActivity().runOnUiThread(() -> {
                recyclerView.setVisibility(View.VISIBLE);
                loadingView.setVisibility(View.GONE);
                errorView.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            });
        }
    }

    @Override
    public void onItemClick(StreamInfoItem item, int position) {
        if(getContext() != null) Toast.makeText(getContext(), "Playing: " + item.getName(), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (loadingThread != null && loadingThread.isAlive()) {
            loadingThread.interrupt();
        }
    }
}