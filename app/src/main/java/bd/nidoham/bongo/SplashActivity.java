package bd.nidoham.bongo;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int INITIAL_SPLASH_DELAY = 1000; // 1 second
    private static final int POST_PERMISSION_DELAY = 1500; // 1.5 seconds
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 101;

    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // প্রথমে 1 সেকেন্ড অপেক্ষা, তারপর পারমিশন চেক
        handler.postDelayed(this::checkStoragePermission, INITIAL_SPLASH_DELAY);
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ MANAGE_EXTERNAL_STORAGE চেক
            if (!Environment.isExternalStorageManager()) {
                // Settings এ নিয়ে যাওয়া লাগবে
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_STORAGE_PERMISSION);
            } else {
                // অনুমতি আছে, 1.5 সেকেন্ড পরে MainActivity
                proceedAfterPermission();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ READ_MEDIA_VIDEO permission চেক
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) != getPackageManager().PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_VIDEO}, REQUEST_CODE_STORAGE_PERMISSION);
            } else {
                proceedAfterPermission();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-12 READ_EXTERNAL_STORAGE permission চেক
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != getPackageManager().PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSION);
            } else {
                proceedAfterPermission();
            }
        } else {
            // Android <6 কোন permission লাগবে না
            proceedAfterPermission();
        }
    }

    private void proceedAfterPermission() {
        // পারমিশন মঞ্জুর হলে 1.5 সেকেন্ড Splash দেখাবে তারপর MainActivity যাবে
        handler.postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, POST_PERMISSION_DELAY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            // MANAGE_EXTERNAL_STORAGE permission এর ফলাফল
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    proceedAfterPermission();
                } else {
                    Toast.makeText(this, "Storage permission is required.", Toast.LENGTH_LONG).show();
                    // আপনি চাইলে আবার permission চাওয়ার লজিক দিতে পারেন অথবা অ্যাপ বন্ধ করতে পারেন
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == getPackageManager().PERMISSION_GRANTED) {
                proceedAfterPermission();
            } else {
                Toast.makeText(this, "Storage permission is required.", Toast.LENGTH_LONG).show();
                // পারমিশন না দিলে এখানে ইচ্ছা মতো হ্যান্ডেল করুন
            }
        }
    }
}
