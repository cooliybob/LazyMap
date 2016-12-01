package com.example.acer.lazymap;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;
import com.example.bob.download.DownloadManager;
import com.example.bob.download.DropboxConstant;
import com.example.bob.download.TaskDelegate;
import com.google.gson.Gson;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.rendertheme.AssetsRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.mapsforge.MapsForgeTileProvider;
import org.osmdroid.mapsforge.MapsForgeTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.io.FileFilter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements TaskDelegate {

    private static final int MY_PERMISSIONS_REQUEST = 123;
    private static final int ZOOMLEVEL = 15;
    private static final DecimalFormat df = new DecimalFormat("#.000");
    private MapView mapView;
    private File[] files;
    private IMapController mapController;
    private LocationManager locationManager;
    private CompassOverlay mCompassOverlay = null;
    private MyLocationNewOverlay myLocationoverlay = null;
    private XmlRenderTheme theme = null;
    private String type = "Elevate";
    private GeoPoint myLocation = new GeoPoint(25.03, 121.38);
    private SharedPreferences mPrefs;
    private ProgressBar mProgressBar;
    private ScaleBarOverlay mScaleBarOverlay;
    private RotationGestureOverlay mRotationGestureOverlay;
    private TextView textViewCurrentLocation;
    private DropboxAPI<AndroidAuthSession> mDBApi;
    private AppBarLayout appBarLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        mProgressBar = (ProgressBar) findViewById(R.id.float_progressbar);
        textViewCurrentLocation = (TextView) findViewById(R.id.textViewCurrentLocation);
        appBarLayout = (AppBarLayout) findViewById(R.id.appBar);
        setSupportActionBar(toolbar);
        initDropbox();

        mPrefs = getPreferences(MODE_PRIVATE);

        AndroidGraphicFactory.createInstance(this.getApplication());
        mapView = (MapView) findViewById(R.id.map);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.setUseDataConnection(false);

        mapController = mapView.getController();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION},
                MY_PERMISSIONS_REQUEST);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myLocationoverlay.runOnFirstFix(new Runnable() {
                    public void run() {
                        mapController.animateTo(myLocation);
                    }
                });
                mapController.setZoom(ZOOMLEVEL);
                mRotationGestureOverlay.resetMapAngle();
            }
        });
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        if (mDBApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                mDBApi.getSession().finishAuthentication();
                String accessToken = mDBApi.getSession().getOAuth2AccessToken();

                new DownloadManager(this, this).downloadFromDropbox(mDBApi);

            } catch (IllegalStateException e) {
                Log.e("Dropbox", "Error authenticating", e);
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLocationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, myLocationListener);

        getData();
        setMyLocationOverlay();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        saveData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.removeUpdates(myLocationListener);
        myLocationoverlay.disableMyLocation();
    }

    private void saveData() {
        Gson gson = new Gson();
        String json = gson.toJson(myLocation);
        mPrefs.edit().putString("MyLastLocation", json).commit();
    }

    private void getData() {
        Gson gson = new Gson();
        String json = mPrefs.getString("MyLastLocation", "");
        if (json != "") {
            myLocation = gson.fromJson(json, GeoPoint.class);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Set<File> mapfiles = findMapFiles();
                    if (mapfiles.size() != 0) {
                        files = new File[mapfiles.size()];
                        files = mapfiles.toArray(files);
                        setOfflineMapAndTheme();
                    } else {
                        Toast.makeText(this, "No .map be found", Toast.LENGTH_SHORT).show();
                    }
                    setCompassOverlay();
                    setRotationGestureOverlay();
                    setCenterOverlay();
                    setCenterLocationListener();
//                    setScaleBarOverlay();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        String type = null;
        switch (item.getItemId()) {
            case R.id.action_download_map:
//                mapView.setTileSource(new XYTileSource("Mapnik", 0, 15, 256, ".jpg", new String[] {}));
//                mapView.invalidate();

//                new DownloadManager(this, this).downloadFromFTP(MapConstant.DOWNLOAD_URL);
                mDBApi.getSession().startOAuth2Authentication(MainActivity.this);

                return true;
            case R.id.item_show_compass:
                Toast.makeText(this,"Show compass",Toast.LENGTH_SHORT).show();
                setBigCompassView();
                return true;
            case R.id.action_elevate_map:
                type = "Elevate";
                break;
            case R.id.action_normal_map:
                type = "Normal";
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        if (this.type != type) {
            new ChangeThemeTask().execute(type);
            mProgressBar.setVisibility(View.VISIBLE);
        }

        return true;
    }

    private void initDropbox() {
        AppKeyPair appKeys = new AppKeyPair(DropboxConstant.APP_KEY, DropboxConstant.APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        mDBApi = new DropboxAPI(session);
    }

    private LocationListener myLocationListener
            = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            // TODO Auto-generated method
            int mLatitude = (int) (location.getLatitude() * 1E6);
            int mLongtitude = (int) (location.getLongitude() * 1E6);
            myLocation = new GeoPoint(mLatitude, mLongtitude);
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub

        }
    };

    private MapListener mapListener
            = new MapListener() {
        @Override
        public boolean onScroll(ScrollEvent event) {
            updateInfo();
            return false;
        }

        @Override
        public boolean onZoom(ZoomEvent event) {
            updateInfo();
            return false;
        }
    };

    @Override
    public void taskCompletionResult(boolean result) {
        if (result) {
            Toast.makeText(this, "Download complete", Toast.LENGTH_SHORT).show();

            Set<File> mapfiles = findMapFiles();
            files = new File[mapfiles.size()];
            files = mapfiles.toArray(files);
            setOfflineMapAndTheme();

            //Close dropbox link when download finish
            mDBApi.getSession().unlink();
        }
    }

    private class ChangeThemeTask extends AsyncTask<String, Void, MapsForgeTileProvider> {

        @Override
        protected MapsForgeTileProvider doInBackground(String... params) {

            try {
                theme = new AssetsRenderTheme(getApplicationContext(), "renderthemes/", params[0] + ".xml");
                type = params[0];
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            MapsForgeTileProvider forge = new MapsForgeTileProvider(
                    new SimpleRegisterReceiver(MainActivity.this),
                    MapsForgeTileSource.createFromFiles(files, theme, type));
            return forge;
        }

        protected void onPostExecute(MapsForgeTileProvider result) {
            mapView.setTileProvider(result);
            mapView.invalidate();
            mProgressBar.setVisibility(View.GONE);
        }
    }

    protected static Set<File> findMapFiles() {
        Set<File> maps = new HashSet<>();
        List<StorageUtils.StorageInfo> storageList = StorageUtils.getStorageList();
        for (int i = 0; i < storageList.size(); i++) {
            File f = new File(storageList.get(i).path + File.separator + "osmdroid" + File.separator);
            if (f.exists()) {
                maps.addAll(scan(f));
            }
        }
        return maps;
    }

    static private Collection<? extends File> scan(File f) {
        List<File> ret = new ArrayList<>();
        File[] files = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.getName().toLowerCase().endsWith(".map"))
                    return true;
                return false;
            }
        });
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                ret.add(files[i]);
            }
        }
        return ret;
    }

    private void setOfflineMapAndTheme() {
        try {
            theme = new AssetsRenderTheme(getApplicationContext(), "renderthemes/", type + ".xml");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        MapsForgeTileProvider forge = new MapsForgeTileProvider(
                new SimpleRegisterReceiver(this),
                MapsForgeTileSource.createFromFiles(files, theme, type));
        mapView.setTileProvider(forge);
        mapView.invalidate();
    }

    private void setMyLocationOverlay() {
        myLocationoverlay = new MyLocationNewOverlay(mapView);
        myLocationoverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationoverlay);
        myLocationoverlay.setDrawAccuracyEnabled(true);

        mapController.setZoom(ZOOMLEVEL);

        if (myLocationoverlay.getMyLocation() != null) {
            myLocationoverlay.runOnFirstFix(new Runnable() {
                public void run() {
                    mapController.animateTo(myLocationoverlay.getMyLocation());
                }
            });
        } else {
            mapController.setCenter(myLocation);
        }
        mapView.invalidate();
    }

    private void setCompassOverlay() {
        mCompassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this),
                mapView);
        mCompassOverlay.enableCompass();
        mapView.getOverlays().add(mCompassOverlay);
//        mCompassOverlay.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                setBigCompassView();
//            }
//        });
//        mCompassOverlay.onSingleTapUp()
    }

    private void setScaleBarOverlay() {
        final DisplayMetrics dm = this.getResources().getDisplayMetrics();
        mScaleBarOverlay = new ScaleBarOverlay(mapView);
        mScaleBarOverlay.setAlignBottom(true);
        mScaleBarOverlay.setAlignRight(true);
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);
        mapView.getOverlays().add(this.mScaleBarOverlay);
    }

    private void setCenterOverlay() {
        CoordinatorLayout root = (CoordinatorLayout) findViewById(R.id.root_layout);
        ImageView image = new ImageView(MainActivity.this);
        image.setImageResource(R.drawable.center);
        root.addView(image);
        CoordinatorLayout.LayoutParams img = (CoordinatorLayout.LayoutParams) image.getLayoutParams();
        img.gravity = Gravity.CENTER;
        image.setLayoutParams(img);
    }

    private void setBigCompassView() {
        Intent i= new Intent(this, CompassActivity.class);
        startActivity(i);
    }

    private void setCenterLocationListener() {
        mapView.setMapListener(mapListener);
        updateInfo();
    }

    private void setRotationGestureOverlay() {
        mRotationGestureOverlay = new RotationGestureOverlay(this, mapView);
        mRotationGestureOverlay.setEnabled(true);
        mapView.getOverlays().add(this.mRotationGestureOverlay);
    }

    private void updateInfo() {
        IGeoPoint mapCenter = mapView.getMapCenter();
        textViewCurrentLocation.setText("WGS: ("+
                df.format(mapCenter.getLongitude())+" , "+
                df.format(mapCenter.getLatitude())+")");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        String DEBUG_TAG= "onTouchEvent";
        switch (action) {
            case (MotionEvent.ACTION_DOWN) :
                Log.e(DEBUG_TAG,"Action was DOWN");
                return true;
            case (MotionEvent.ACTION_MOVE) :
                Log.e(DEBUG_TAG,"Action was MOVE");
                appBarLayout.setExpanded(false,true);
                return true;
            case (MotionEvent.ACTION_UP) :
                appBarLayout.setExpanded(true,true);
                Log.e(DEBUG_TAG,"Action was UP");
                return true;
            case (MotionEvent.ACTION_CANCEL) :
                Log.e(DEBUG_TAG,"Action was CANCEL");
                return true;
            case (MotionEvent.ACTION_OUTSIDE) :
                Log.e(DEBUG_TAG,"Movement occurred outside bounds " +
                        "of current screen element");
                return true;
            default :
                return super.onTouchEvent(event);
        }
    }
}
