package bd.nidoham.bongo.cache;

import android.content.Context;
import android.content.SharedPreferences;

public class UserSessionManager {

    private static final String PREFS_NAME = "UserSessionPrefs";
    private static final String KEY_YOUTUBE_COOKIES = "youtube_cookies";
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public UserSessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void saveSession(String cookies) {
        editor.putString(KEY_YOUTUBE_COOKIES, cookies);
        editor.apply();
    }

    public String getSessionCookies() {
        return sharedPreferences.getString(KEY_YOUTUBE_COOKIES, null);
    }

    public boolean isLoggedIn() {
        return getSessionCookies() != null && !getSessionCookies().isEmpty();
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}