package bd.nidoham.bongo.list.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import bd.nidoham.bongo.R;

public class YoutubeAdapter extends ListAdapter<StreamInfoItem, YoutubeAdapter.VideoViewHolder> {

    private final LayoutInflater inflater;
    private final Picasso picasso;
    private OnItemClickListener onItemClickListener;
    
    // Business efficiency: Pre-format common strings to avoid repeated calculations
    private static final String VIEWS_SUFFIX = " views";
    private static final String SEPARATOR = " • ";
    private static final String[] VIEW_COUNT_UNITS = {"", "K", "M", "B", "T", "P", "E"};
    
    // Interface for click handling - business requirement for user interaction
    public interface OnItemClickListener {
        void onItemClick(StreamInfoItem item, int position);
    }

    public YoutubeAdapter(Context context) {
        super(new VideoDiffCallback());
        this.inflater = LayoutInflater.from(context);
        this.picasso = Picasso.get(); // Reuse instance for better performance
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        holder.setAdapter(this); // Set adapter reference for click handling
        holder.bind(getItem(position), picasso);
    }

    // Business efficiency: Use DiffUtil for better performance with large datasets
    private static class VideoDiffCallback extends DiffUtil.ItemCallback<StreamInfoItem> {
        @Override
        public boolean areItemsTheSame(@NonNull StreamInfoItem oldItem, @NonNull StreamInfoItem newItem) {
            return TextUtils.equals(oldItem.getUrl(), newItem.getUrl());
        }

        @Override
        public boolean areContentsTheSame(@NonNull StreamInfoItem oldItem, @NonNull StreamInfoItem newItem) {
            return TextUtils.equals(oldItem.getName(), newItem.getName()) &&
                   TextUtils.equals(oldItem.getUploaderName(), newItem.getUploaderName()) &&
                   oldItem.getViewCount() == newItem.getViewCount() &&
                   oldItem.getDuration() == newItem.getDuration();
        }
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView thumbnailImageView;
        private final TextView titleTextView;
        private final TextView uploaderTextView;
        private final TextView detailsTextView;
        private final TextView durationTextView;
        private final ImageView channelAvatar;
        private final ImageView moreOptions;
        
        // Business efficiency: Reuse StringBuilder to avoid object creation
        private final StringBuilder stringBuilder = new StringBuilder();
        
        // Store reference for click handling
        private OnItemClickListener clickListener;
        private YoutubeAdapter adapter;

        VideoViewHolder(@NonNull View itemView, OnItemClickListener clickListener) {
            super(itemView);
            thumbnailImageView = itemView.findViewById(R.id.videoThumbnail);
            titleTextView = itemView.findViewById(R.id.videoTitle);
            uploaderTextView = itemView.findViewById(R.id.videoUploader);
            detailsTextView = itemView.findViewById(R.id.videoDetails);
            durationTextView = itemView.findViewById(R.id.videoDuration);
            channelAvatar = itemView.findViewById(R.id.channelAvatar);
            moreOptions = itemView.findViewById(R.id.moreOptions);
            
            this.clickListener = clickListener;
            
            // Business requirement: Handle item clicks for user engagement
            if (clickListener != null) {
                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && adapter != null) {
                        StreamInfoItem currentItem = adapter.getItem(position);
                        clickListener.onItemClick(currentItem, position);
                    }
                });
            }
            
            // Handle more options click separately
            if (moreOptions != null) {
                moreOptions.setOnClickListener(v -> {
                    // Handle more options menu - can be implemented as needed
                    // For now, just show/hide for business requirements
                    moreOptions.setVisibility(View.GONE);
                });
            }
        }
        
        // Method to set adapter reference for click handling
        void setAdapter(YoutubeAdapter adapter) {
            this.adapter = adapter;
        }

        void bind(@NonNull final StreamInfoItem item, @NonNull Picasso picasso) {
            // Business efficiency: Set text directly without null checks (handled by TextView)
            titleTextView.setText(item.getName());
            uploaderTextView.setText(item.getUploaderName());

            // Efficient detail string building
            buildDetailsString(item);
            detailsTextView.setText(stringBuilder.toString());

            // Handle duration display
            setDurationText(item.getDuration());

            // Optimized image loading
            loadThumbnail(item, picasso);
            
            // Handle channel avatar - for business requirements, currently hidden
            if (channelAvatar != null) {
                channelAvatar.setVisibility(View.GONE);
            }
            
            // Handle more options - for business requirements, currently hidden
            if (moreOptions != null) {
                moreOptions.setVisibility(View.GONE);
            }
        }

        private void buildDetailsString(@NonNull StreamInfoItem item) {
            stringBuilder.setLength(0); // Clear previous content
            
            if (item.getViewCount() >= 0) {
                stringBuilder.append(formatViewCount(item.getViewCount()))
                           .append(VIEWS_SUFFIX);
            }
            
            try {
                String uploadDate = item.getTextualUploadDate();
                if (!TextUtils.isEmpty(uploadDate)) {
                    if (stringBuilder.length() > 0) {
                        stringBuilder.append(SEPARATOR);
                    }
                    stringBuilder.append(uploadDate);
                }
            } catch (Exception ignored) {
                // Business decision: Continue gracefully on date parsing errors
            }
        }

        private void setDurationText(long duration) {
            if (duration > 0) {
                durationTextView.setText(formatDuration(duration));
                durationTextView.setVisibility(View.VISIBLE);
            } else {
                durationTextView.setVisibility(View.GONE);
            }
        }

        private void loadThumbnail(@NonNull StreamInfoItem item, @NonNull Picasso picasso) {
            String thumbnailUrl = getBestThumbnailUrl(item);
            
            if (!TextUtils.isEmpty(thumbnailUrl)) {
                RequestCreator request = picasso.load(thumbnailUrl)
                        .placeholder(R.drawable.placeholder_thumbnail_video)
                        .error(R.drawable.placeholder_thumbnail_video)
                        .fit()
                        .centerCrop();
                
                // Business efficiency: Add memory optimization
                request.memoryPolicy(com.squareup.picasso.MemoryPolicy.NO_CACHE, 
                                   com.squareup.picasso.MemoryPolicy.NO_STORE);
                request.into(thumbnailImageView);
            } else {
                thumbnailImageView.setImageResource(R.drawable.placeholder_thumbnail_video);
            }
        }

        private String getBestThumbnailUrl(@NonNull StreamInfoItem item) {
            if (item.getThumbnails() == null || item.getThumbnails().isEmpty()) {
                return null;
            }
            // Business decision: Use highest quality thumbnail available
            return item.getThumbnails().get(item.getThumbnails().size() - 1).getUrl();
        }

        // Business efficiency: Optimized view count formatting with fewer calculations
        private String formatViewCount(long count) {
            if (count < 1000) return String.valueOf(count);
            
            int unitIndex = 0;
            double value = count;
            
            while (value >= 1000 && unitIndex < VIEW_COUNT_UNITS.length - 1) {
                value /= 1000;
                unitIndex++;
            }
            
            // Format with appropriate decimal places based on business requirements
            if (value >= 100) {
                return String.format(Locale.US, "%.0f%s", value, VIEW_COUNT_UNITS[unitIndex]);
            } else {
                return String.format(Locale.US, "%.1f%s", value, VIEW_COUNT_UNITS[unitIndex]);
            }
        }

        // Business efficiency: Use TimeUnit for better readability and performance
        private String formatDuration(long seconds) {
            long hours = TimeUnit.SECONDS.toHours(seconds);
            long minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60;
            long secs = seconds % 60;
            
            return hours > 0 ?
                    String.format(Locale.US, "%d:%02d:%02d", hours, minutes, secs) :
                    String.format(Locale.US, "%02d:%02d", minutes, secs);
        }
    }
}