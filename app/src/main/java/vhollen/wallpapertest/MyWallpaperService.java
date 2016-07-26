package vhollen.wallpapertest;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.service.wallpaper.WallpaperService;
import android.support.v4.app.ActivityCompat;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MyWallpaperService extends WallpaperService {
    public List<String> bluetoothScan = new ArrayList<>();

    @Override
    public Engine onCreateEngine() {
        return new MyWallpaperEngine();
    }

    private class MyWallpaperEngine extends Engine implements LocationListener {

        int height, width;
        private boolean visible = true;
        int counter = 0;

        private LocationManager locationManager;
        private String provider;
        private Location location, loc;


        private WifiManager wifi;
        private List<ScanResult> wifiScan = new ArrayList<>();


        private BluetoothAdapter bluetoothAdapter;
        private SingBroadcastReceiver mReceiver;

        private MediaRecorder mRecorder = null;


        private final Handler mHandler = new Handler();

        private final Runnable mUpdateDisplay = new Runnable() {

            @Override

            public void run() {

                draw();

            }
        };


        @SuppressWarnings("deprecation")
        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {

            //-------------------------------------location stuff-------------------------------------------------------
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            Criteria criteria = new Criteria();

            provider = locationManager.getBestProvider(criteria, true);


            if(!locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER)){
                Toast.makeText(getApplicationContext(), "Someone wants to know your Location!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

            if (ActivityCompat.checkSelfPermission(MyWallpaperService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MyWallpaperService.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


                return;
            }
            locationManager.requestLocationUpdates(provider, 100, 0, this);
            location = locationManager.getLastKnownLocation(provider);
            loc = location;
            onLocationChanged(location);

            //--------------------Wi-Fi stuff---------------------------------------

            wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            if (wifi.isWifiEnabled() == false)
            {
                wifi.setWifiEnabled(true);
            }

            wifi.startScan();

            //--------------------Bluetooth stuff---------------------------

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter.isEnabled()){
                System.out.println("Bluetooth is Enabled.");
            }else{
                System.out.println("Bluetooth is NOT Enabled!");

                Toast.makeText(getApplicationContext(), "Someone wants to know Bluetooth-devices in the surrounding!", Toast.LENGTH_SHORT).show();
                Intent enableBtIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(enableBtIntent);
            }

            mReceiver = new SingBroadcastReceiver();
            IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, intent);


            //-----------------------------Microphone stuff----------------------------------

            if (mRecorder == null) {
                mRecorder = new MediaRecorder();
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mRecorder.setOutputFile("/dev/null");

                mRecorder.setOnErrorListener(errorListener);
                mRecorder.setOnInfoListener(infoListener);

                try {
                    mRecorder.prepare();
                    mRecorder.start();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private MediaRecorder.OnErrorListener errorListener = new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                System.out.println("Error: " + what + ", " + extra);
            }
        };

        private MediaRecorder.OnInfoListener infoListener = new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                System.out.println("Warning: " + what + ", " + extra);
            }
        };

        @Override
        public void onDestroy() {
            visible = false;

            mHandler.removeCallbacks(mUpdateDisplay);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (!visible) {
                if (mRecorder != null) {
                    mRecorder.reset();
                    mRecorder.release();
                    mRecorder = null;
                }
                mHandler.removeCallbacks(mUpdateDisplay);

            } else {
                if (ActivityCompat.checkSelfPermission(MyWallpaperService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MyWallpaperService.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                locationManager.requestLocationUpdates(provider, 100, 0, this);

                if (wifi.isWifiEnabled() == false)
                {
                    wifi.setWifiEnabled(true);
                }

                if (mRecorder == null) {
                    mRecorder = new MediaRecorder();
                    mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    mRecorder.setOutputFile("/dev/null");

                    mRecorder.setOnErrorListener(errorListener);
                    mRecorder.setOnInfoListener(infoListener);

                    try {
                        mRecorder.prepare();
                        mRecorder.start();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                mHandler.post(mUpdateDisplay);
            }
        }

            @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            this.visible = false;
            if (mRecorder != null) {
                mRecorder.reset();
                mRecorder.release();
                mRecorder = null;
            }
            mHandler.removeCallbacks(mUpdateDisplay);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format,
                                     int width, int height) {
            this.width = width;
            this.height = height;
            super.onSurfaceChanged(holder, format, width, height);

            if(isVisible()){
                if (ActivityCompat.checkSelfPermission(MyWallpaperService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MyWallpaperService.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                locationManager.requestLocationUpdates(provider, 100, 0, this);

                if (wifi.isWifiEnabled() == false)
                {
                    wifi.setWifiEnabled(true);
                }

                /*if (mRecorder == null) {
                    mRecorder = new MediaRecorder();
                    mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    mRecorder.setOutputFile("/dev/null");

                    mRecorder.setOnErrorListener(errorListener);
                    mRecorder.setOnInfoListener(infoListener);

                    try {
                        mRecorder.prepare();
                        mRecorder.start();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }*/

                mHandler.post(mUpdateDisplay);
            }
        }

        @Override
        public void onTouchEvent(MotionEvent event) {

        }


        //-------------------------------------------draw function--------------------------
        private void draw() {

            SurfaceHolder holder = getSurfaceHolder();

            Canvas c = null;

            try {

                c = holder.lockCanvas();

                if (c != null) {
                    counter++;

                    int x = 0;
                    int y = 0;

                    Paint p = new Paint();
                    p.setAntiAlias(true);

                    p.setColor(Color.BLACK);
                    c.drawRect(0, 0, c.getWidth(), c.getHeight(), p);


                    int amp = (int) getAmplitude()/250;
                    //System.out.println(amp);

                    p.setColor(Color.rgb(255,150,0));

                    for(int i = 1; i<=amp;i++){
                        x = c.getWidth()/2-200;
                        y = c.getHeight()-i*50;
                        c.drawRect(x,y,x+400,y+45,p);
                    }
                    p.setColor(Color.RED);


                    p.setTextSize(20);
                    x=20;
                    y=c.getHeight()/10;
                    c.drawText("I'm just an ordinary Wallpaper, not collecting any Data!!", x, y, p);


                    p.setTextSize(30);
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String formattedDate = df.format(cal.getTime());

                    float w = p.measureText(formattedDate, 0, formattedDate.length());
                    int offset = (int) w / 2;

                    x = c.getWidth()/2 - offset;
                    y = c.getHeight()/6;

                    p.setColor(Color.GRAY);
                    c.drawText("Clock:", x, y, p);
                    y+= 50;
                    c.drawText(formattedDate, x, y, p);

                    //Location loc = locationManager.getLastKnownLocation(provider);
                    //System.out.println( "location    " + loc.getLatitude() + ", " + loc.getLongitude());
                    String text = "";
                    if(loc != null) {
                         text = loc.getLatitude() + ", " + loc.getLongitude();
                    }

                    p.setColor(Color.CYAN);


                    w = p.measureText(text, 0, text.length());
                    offset = (int) w / 2;

                    x = c.getWidth()/2 - offset;
                    y = c.getHeight()/6+150;

                    c.drawText(text, x, y, p);
                    c.drawText("Location:", x, y-50, p);

                    if(counter%10 == 0){
                        //wifi.startScan();
                        wifiScan = wifi.getScanResults();
                    }

                    if (counter % 20 == 0) {
                        bluetoothAdapter.startDiscovery();
                        //System.out.println(bluetoothScan.size());

                    }
                    if (counter == 100) {

                        bluetoothScan.clear();
                        counter = 0;
                    }

                    p.setTextSize(20);
                    //x = c.getWidth()/2;
                    x = 20;
                    y += 100;

                    p.setColor(Color.GREEN);

                    c.drawText("WiFi:", x, y, p);
                    y += 50;


                    if(wifiScan.size()>0){
                        for (int i = 0; i < wifiScan.size();i++){
                            String outWifi = wifiScan.get(i).BSSID + ", " + wifiScan.get(i).SSID;
                            c.drawText(outWifi, x, y, p);
                            y += 50;
                        }
                    }

                    x = c.getWidth()/2;
                    y = c.getHeight()/6 + 250;

                    p.setColor(Color.BLUE);
                    c.drawText("Bluetooth:", x, y, p);
                    y += 50;


                    if(bluetoothScan.size()>0){
                        for (int i = bluetoothScan.size()-1; i >=0;i--){
                            String outWifi = bluetoothScan.get(i);
                            c.drawText(outWifi, x, y, p);
                            y += 50;
                        }
                    }


                }

            } finally {

                if (c != null)

                    holder.unlockCanvasAndPost(c);

            }

            mHandler.removeCallbacks(mUpdateDisplay);

            if (visible) {

                mHandler.postDelayed(mUpdateDisplay, 200);

            }

        }


        public double getAmplitude() {
            if (mRecorder != null)
                return  mRecorder.getMaxAmplitude();
            else
                return 0;

        }


        @Override
        public void onLocationChanged(Location location) {
            loc = location;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    private class SingBroadcastReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); //may need to chain this to a recognizing function
            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a Toast
                boolean alreadyin = false;

                for (int i = 0; i < bluetoothScan.size(); i++) {
                    if (bluetoothScan.get(i).contains(device.getAddress())) {
                        alreadyin = true;
                    }
                }


                if(!alreadyin){
                    bluetoothScan.add(device.getName() + ", " + device.getAddress());
                    //System.out.println("Bluetooth found something!");
                }
            }
        }
    }

}

