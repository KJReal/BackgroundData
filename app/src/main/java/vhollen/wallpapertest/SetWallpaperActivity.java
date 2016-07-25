package vhollen.wallpapertest;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class SetWallpaperActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_wallpaper);



        TextView txt = (TextView) findViewById(R.id.textView);
        txt.setText("This Wallpaper is just here for showing how easy any app can get all your information. It simultaneously checks your clock, your location, your available Wi-Fi Networks, open Bluetooth connections and your microphone. In the case of this Wallpaper, it doesn’t send this data anywhere, but it makes you aware of what and how easy all this information is accessible.\n\n" +
                "The Source Code is available on GitHub (Link follows)\n\n" +
                "Best, KJ\n");
    }

    public void onClick(View view) {
        Intent intent = new Intent(
                WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                new ComponentName(this, MyWallpaperService.class));
        startActivity(intent);
    }
}