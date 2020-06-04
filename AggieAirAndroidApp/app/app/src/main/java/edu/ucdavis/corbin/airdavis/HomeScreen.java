package edu.ucdavis.corbin.airdavis;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.mongodb.lang.NonNull;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.core.auth.StitchUser;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteFindIterable;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;

import org.bson.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

 /**
  * Home Screen
  *
  * The home screen is the app landing page.
  * It has a map display of davis with changeable date that updates with pm25 information of that date.
  * It requests user bluetooth permissions as well as location permissions.
  * Binds a Uart service to it for Bluetooth communication.
  * Provides a live readout of sensor data if connected.
  * Provides links to Device settings activity.
  */
public class HomeScreen extends AppCompatActivity implements OnMapReadyCallback, RadioGroup.OnCheckedChangeListener  {
    private static final int REQUEST_SELECT_DEVICE = 1; // request id to handle response
    public static final int REQUEST_ENABLE_BT = 2; // request id to handle response
    private static final int UART_PROFILE_READY = 10; // Codes the Uart service sends
    private static final int UART_PROFILE_CONNECTED = 20; // Codes the Uart service sends
    private static final int UART_PROFILE_DISCONNECTED = 21; // Codes the Uart service sends
    private static final int STATE_OFF = 10; // Codes the Uart service sends
    public static final String TAG = "aggieair";

    private BluetoothAdapter bluetoothAdapter; // contains phone BLE status information
    private BLE_Device mDevice; // BLE device to store sensor connection
    private UartService mService; // Service that manages incoming BLE communication

    private GoogleMap mMap; // Map for data display from DB
    private RemoteMongoCollection<Document> sensorCollection; // Stores a connection to a remote mongo collection
    private RemoteMongoClient mongoClient; // Stores connection information about a remote mongo client
    private StitchAppClient stitchClient; // Used to establish Mongo connection

     // Binding of Gmap so that users don't scroll too far
    private LatLngBounds ucDavisBinding = new LatLngBounds(new LatLng(38.526799, -121.767531),  //SW
                                                           new LatLng(38.545989, -121.745370)); //NE

    // store chosen date
    private String txtDate = new SimpleDateFormat("ddMMyy",Locale.getDefault()).format(new Date()); // saves current date as string

    TextView pmRead; // text display of sensor data
    TextView LoadingMap; // text display of loading status
    TextView dateDisplay; // text display of chosen date
    ImageButton dateBtn; // btn for choosing date through calender dialog
    private boolean mapUpdate =false; // flag for if the map needs to render data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        // Grab layout values we need
        LoadingMap = (TextView) findViewById(R.id.LoadingMap);

        TextView Title = (TextView) findViewById(R.id.title); // set title with two color font
        Title.setText(Html.fromHtml("<font color='#F2F2F2'>aggie</font><font color='#E88A8A8A'>air</font>"));

        dateDisplay = (TextView) findViewById(R.id.dateDisplay);
        setDateString();

        dateBtn = (ImageButton) findViewById(R.id.dateBtn);
        dateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDateDialog(); // launch calender
            }
        });

        pmRead = (TextView) findViewById(R.id.pmVal);

        ImageButton deviceSettings = (ImageButton) findViewById(R.id.settingsBtn);
        deviceSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDeviceDialog();
            }
        });

        final BluetoothManager bluetoothManager = // grab devices BLE manager to ensure Bluetooth is enabled
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter(); // adapter is how we interface with the phones BLE system
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); // request BLE if BLE is inactive
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        service_init(); // set up Uart service
        // Establish map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        // DB
        stitchClient = Stitch.getDefaultAppClient();
        stitchClient.getAuth().loginWithCredential(new AnonymousCredential())
                .addOnCompleteListener(new OnCompleteListener<StitchUser>() {
                    @Override
                    public void onComplete(@NonNull Task<StitchUser> task) {
                        mongoClient = stitchClient.getServiceClient(RemoteMongoClient.factory, "mongodb-atlas");
                        sensorCollection = mongoClient.getDatabase(getResources().getString(R.string.data_base_name))
                                .getCollection(getResources().getString(R.string.collection_name));
                        getTodaysData(); // once db connection is established, grab todays data and render it
                    }
                });
    }

    // function for turning ddMMyy into MM/dd/yy
    private void setDateString() {
        String display = "";
        if(txtDate == null || txtDate.length() <6) return;
        display += txtDate.substring(2,4)+"/";
        display += txtDate.substring(0,2)+"/";
        display += txtDate.substring(4);
        dateDisplay.setText(display);
    }

    // function for opening calendar dialog
    private void openDateDialog() {
        if(txtDate == null) return; // sanity check
        int mYear,mMonth,mDay;
        mYear = Integer.parseInt(txtDate.substring(4)) + 2000; // get int values from last date set
        mMonth = Integer.parseInt(txtDate.substring(2,4)) - 1;
        mDay = Integer.parseInt(txtDate.substring(0,2));

        DatePickerDialog picker = new DatePickerDialog(this,
            new DatePickerDialog.OnDateSetListener() {
            // on date picked, store value in date string
            @Override
            public void onDateSet(DatePicker view, int year,
                                  int monthOfYear, int dayOfMonth) {
                String yearS = Integer.toString(year).substring(2);// last two digits of year
                String monthS = monthOfYear+1 > 9 ? Integer.toString(monthOfYear+1) : "0" + Integer.toString(monthOfYear+1);
                String dayS = dayOfMonth > 9 ? Integer.toString(dayOfMonth) : "0" + Integer.toString(dayOfMonth);
                txtDate = (dayS+monthS+yearS);
                setDateString(); // update display string
                getTodaysData(); // update data for map

            }
        }, mYear, mMonth, mDay);
        picker.show();
    }

    // function  for opening device settings activity
    public void openDeviceDialog() {
        Intent deviceWindow = new Intent(getApplicationContext(), DeviceSettings.class);
        startActivityForResult(deviceWindow, REQUEST_SELECT_DEVICE);
    }

    // Bind the Uaet service
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };

    // function for handling response from device when asking for permissions or asking for device
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE: // Device settings response
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = new BLE_Device(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress));
                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    mService.connect(deviceAddress); // set bound service device to chosen device
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON, sensor connection will not be available ", Toast.LENGTH_SHORT).show();
                    //finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    // function for handling Uart service messages
    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
            //*********************//
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Utils.toast(getApplicationContext(), "Gatt Connected");
                        pmRead.setText(R.string.connecting);
                    }
                });
            }

            //*********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Utils.toast(getApplicationContext(), "Gatt Disonnected");
                        pmRead.setText("Connect Device");
                        pmRead.setTextColor(getResources().getColor(R.color.textMain));
                    }
                });
            }

            //*********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) { // device notified phone its capable of uart
                mService.enableTXNotification();
            }
            //*********************//
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) { // sensor data sent
                byte[] data = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                if(data!=null) { // sanity check
                    int pm25 = data[0];
                    if( pm25 < 0) pm25 +=256;  // data is unsigned short, convert signed int back into unsigned

                    // set color
                    pmRead.setText(Integer.toString(pm25));
                    if(pm25 <= 12) {
                        pmRead.setTextColor(getResources().getColor(R.color.good));
                    } else if(pm25 <= 35) {
                        pmRead.setTextColor(getResources().getColor(R.color.moderate));
                    } else if(pm25 <= 55) {
                        pmRead.setTextColor(getResources().getColor(R.color.danger));
                    }  else if(pm25 <= 150){
                        pmRead.setTextColor(getResources().getColor(R.color.hazard));
                    } else {
                        pmRead.setTextColor(getResources().getColor(R.color.extreme));
                    }
                }
            }
            //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
                // user must have chosen wrong device
                Utils.toast(getApplicationContext(),"Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }


        }
    };

    // function for establishing uart service
    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    // ignore all signals not in this filter
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() { // need to disable services on destroy
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        if(mServiceConnection != null) {
            unbindService(mServiceConnection);
            mService.stopSelf();
            mService = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() { // if app was closed, need to reinit service
        service_init();
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() { // if device was paused, make sure BLE is still active
        super.onResume();
        Log.d(TAG, "onResume");
        if (!bluetoothAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

    }

    // function for setting map once established
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setLatLngBoundsForCameraTarget(ucDavisBinding); // binds to davis
        googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                        this, R.raw.map_style)); // map style
        LatLng ucDavis = new LatLng(38.5375166, -121.7574996); // center on davis
        mMap.moveCamera(CameraUpdateFactory.newLatLng(ucDavis)); // move to center
        mMap.animateCamera( CameraUpdateFactory.zoomTo( 14.0f ) ); // zoom in on davis
        if(mapUpdate) { // if db was connected before map established, request data again and draw
            getTodaysData();
        }
    }

    // drawing data on map
    private void drawCircle(LatLng point, int pm25){

        // Instantiating CircleOptions to draw a circle around the marker
        CircleOptions circleOptions = new CircleOptions();

        // Specifying the center of the circle
        circleOptions.center(point);

        // Radius of the circle
        circleOptions.radius(5);

        // Border color of the circle
        if(pm25 <= 12) {
            circleOptions.strokeColor(getResources().getColor(R.color.good));
            circleOptions.fillColor(getResources().getColor(R.color.good));
        } else if(pm25 <= 35) {
            circleOptions.strokeColor(getResources().getColor(R.color.moderate));
            circleOptions.fillColor(getResources().getColor(R.color.moderate));
        } else if(pm25 <= 55) {
            circleOptions.strokeColor(getResources().getColor(R.color.danger));
            circleOptions.fillColor(getResources().getColor(R.color.danger));
        }  else if(pm25 <= 150) {
            circleOptions.strokeColor(getResources().getColor(R.color.hazard));
            circleOptions.fillColor(getResources().getColor(R.color.hazard));
        } else {
            circleOptions.strokeColor(getResources().getColor(R.color.extreme));
            circleOptions.fillColor(getResources().getColor(R.color.extreme));
        }

        // Border width of the circle
        circleOptions.strokeWidth(1);
        // Adding the circle to the GoogleMap
        if(mMap !=null) mMap.addCircle(circleOptions);
        else mapUpdate = true;
    }

    private void getTodaysData() {
        if(mMap != null) mMap.clear(); // clear map of data points because we're rendering a new set
        LoadingMap.setVisibility(View.VISIBLE); // inform user that db is being connected
        RemoteFindIterable<Document> query = sensorCollection.find(new Document("DATE",txtDate)).limit(1000); // grabs max 100 points of the same day
        ArrayList<Document> result = new ArrayList<Document>();
        query.into(result).addOnSuccessListener(new OnSuccessListener<Collection<Document>>() {
            @Override
            public void onSuccess(Collection<Document> documents) {
                for(Document doc:documents) {
                    double lat, lon;
                    int pm25;
                    LatLng location;
                    if(doc.containsKey("GPGLL") && doc.containsKey("PM25")) {
                        String loc = doc.getString("GPGLL");
                        lat = Double.parseDouble(loc.split(",")[0]);
                        lon = Double.parseDouble(loc.split(",")[1]);
                        location = new LatLng(lat,lon);
                        pm25 = doc.getInteger("PM25");
                        drawCircle(location, pm25);
                    }
                }
                LoadingMap.setVisibility(View.INVISIBLE); // clear Loading
            }
        });
    }
}