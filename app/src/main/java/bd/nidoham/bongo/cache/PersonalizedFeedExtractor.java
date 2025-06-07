package bd.nidoham.bongo.cache;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.Image.ResolutionLevel;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PersonalizedFeedExtractor {

    private static final String TAG = "PersonalizedFeed";
    private static final String YOUTUBE_BASE_URL = "https://www.youtube.com";
    private final String cookies;

    public PersonalizedFeedExtractor(String cookies) {
        this.cookies = cookies;
    }

    public List<StreamInfoItem> fetchInitialPage() throws IOException {
        try {
            Document doc = Jsoup.connect(YOUTUBE_BASE_URL + "/?persist_gl=1&gl=US")
                    .header("Cookie", cookies)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get();

            for (Element script : doc.select("script")) {
                String scriptData = script.data();
                if (scriptData.contains("var ytInitialData =")) {
                    return parseJsonData(scriptData);
                }
            }
            Log.w(TAG, "ytInitialData not found.");
            return new ArrayList<>();

        } catch (Exception e) {
            Log.e(TAG, "Failed to fetch or parse personalized feed", e);
            throw new IOException("Failed to process personalized feed.", e);
        }
    }

    private List<StreamInfoItem> parseJsonData(String scriptData) {
        List<StreamInfoItem> result = new ArrayList<>();
        try {
            Pattern pattern = Pattern.compile("var ytInitialData = (\\{.*?\\});");
            Matcher matcher = pattern.matcher(scriptData);
            if (!matcher.find()) {
                Log.e(TAG, "Could not find JSON object in the script tag.");
                return result;
            }
            String jsonData = matcher.group(1);
            JSONObject initialData = new JSONObject(jsonData);

            JSONArray contents = initialData.getJSONObject("contents")
                                        .getJSONObject("twoColumnBrowseResultsRenderer")
                                        .getJSONArray("tabs")
                                        .getJSONObject(0)
                                        .getJSONObject("tabRenderer")
                                        .getJSONObject("content")
                                        .getJSONObject("richGridRenderer")
                                        .getJSONArray("contents");

            for (int i = 0; i < contents.length(); i++) {
                try {
                    JSONObject item = contents.getJSONObject(i);
                    if (item.has("richItemRenderer")) {
                        JSONObject videoData = item.getJSONObject("richItemRenderer")
                                                   .getJSONObject("content")
                                                   .getJSONObject("videoRenderer");

                        String videoId = videoData.getString("videoId");
                        String title = videoData.getJSONObject("title").getJSONArray("runs").getJSONObject(0).getString("text");

                        // থাম্বনেইলের JSON থেকে URL, Width এবং Height বের করা হচ্ছে
                        JSONArray thumbnailArray = videoData.getJSONObject("thumbnail").getJSONArray("thumbnails");
                        JSONObject bestThumbnailObject = thumbnailArray.getJSONObject(thumbnailArray.length() - 1);
                        String thumbnailUrl = bestThumbnailObject.getString("url");
                        int thumbnailWidth = bestThumbnailObject.getInt("width");
                        int thumbnailHeight = bestThumbnailObject.getInt("height");

                        StreamInfoItem streamItem = new StreamInfoItem(
                                ServiceList.YouTube.getServiceId(),
                                YOUTUBE_BASE_URL + "/watch?v=" + videoId,
                                title,
                                StreamType.VIDEO_STREAM
                        );

                        // Image অবজেক্টটি প্রয়োজনীয় সব প্যারামিটার দিয়ে তৈরি করা হচ্ছে
                        Image thumbnail = new Image(thumbnailUrl, thumbnailWidth, thumbnailHeight, ResolutionLevel.UNKNOWN);
                        List<Image> thumbnailsList = new ArrayList<>();
                        thumbnailsList.add(thumbnail);

                        streamItem.setThumbnails(thumbnailsList);

                        streamItem.setUploaderName(videoData.getJSONObject("ownerText").getJSONArray("runs").getJSONObject(0).getString("text"));
                        streamItem.setDuration(-1);

                        result.add(streamItem);
                    }
                } catch (Exception e) {
                     Log.w(TAG, "Skipping one item due to parsing error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse ytInitialData JSON", e);
        }

        Log.d(TAG, "Successfully extracted " + result.size() + " videos from JSON.");
        return result;
    }
}