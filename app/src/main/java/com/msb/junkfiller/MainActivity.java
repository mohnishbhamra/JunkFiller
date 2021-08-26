package com.msb.junkfiller;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.navigation.ui.AppBarConfiguration;

import com.msb.junkfiller.databinding.ActivityMainBinding;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    Context context;
    Button button;
    static TextView sizeAvailableTextView;
    static CountDownTimer countDownTimerForSizeUpdateOnTextView;
    static double gbLeft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        button = findViewById(R.id.btn);
        button.setOnClickListener(listener);
        context = this.getApplicationContext();
        sizeAvailableTextView = findViewById(R.id.lblSize);
        countDownTimerForSizeUpdateOnTextView = new CountDownTimer(Integer.MAX_VALUE, 1000) {

            public void onTick(long millisUntilFinished) {
                sizeAvailableTextView.setText(availableSize());
            }

            public void onFinish() {
                sizeAvailableTextView.setText("Phone is full now");
            }


        };
        verifyStoragePermissions(this);


    }

    public static final String prefixFile = "./";
    public static final String fileName = "file";
    public static final String fileNameFormat = ".junk";
    public static final String junkFirstFile = fileName + fileNameFormat;


    View.OnClickListener listener = new View.OnClickListener() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onClick(View view) {
            File sdcard = Environment.getExternalStorageDirectory();
            InputStream inputStream = getResources().openRawResource(R.raw.file);
            try {
                byte[] fileContent = IOUtils.toByteArray(inputStream);
                fillJunk(sdcard, fileContent);
                countDownTimerForSizeUpdateOnTextView.start();
            } catch (Exception exception) {
                cancelCountDownAndSetText();
            }
        }

        void fillJunk(File sdcard, byte[] fileContent) {
            Runnable runnableForJunkCopy = new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void run() {
                    try {
                        while (true) {
                            File fileOut = new File(sdcard, (fileName + (UUID.randomUUID()) + fileNameFormat));
                            fileOut.createNewFile();
                            Files.write(fileOut.toPath(), fileContent);
                        }
                    } catch (Exception e) {
                        cancelCountDownAndSetText();
                    }
                }
            };
            new Thread(runnableForJunkCopy).start();
        }

    };


    static String availableSize() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesAvailable;
        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            bytesAvailable = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
        } else {
            bytesAvailable = (long) stat.getBlockSize() * (long) stat.getAvailableBlocks();
        }
        long megAvailable = bytesAvailable / (1024 * 1024);
        gbLeft = megAvailable / 1024.0;
        return gbLeft + " GB left";
    }

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     * <p>
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }

        sizeAvailableTextView.setText(availableSize());
    }

    void cancelCountDownAndSetText() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                countDownTimerForSizeUpdateOnTextView.cancel();
                sizeAvailableTextView.setText("Phone storage is full now!\nLast step - factory reset your phone for the last time & worry no more.\nBye!");
            }
        });
    }
}