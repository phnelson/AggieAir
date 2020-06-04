package edu.ucdavis.corbin.airdavis;

// Base Stitch Packages

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.mongodb.lang.NonNull;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.core.auth.StitchUser;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteInsertOneResult;

import org.bson.Document;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// Stitch Authentication Packages
// MongoDB Service Packages
// Utility Packages
// Stitch Sync Packages
//import com.mongodb.stitch.core.services.mongodb.remote.sync.internal.ChangeEvent;
//More Mongo Services
//more imports

/**
 * dataBaseService
 *
 * Service for posting finished data points to db
 */
public class dataBaseService extends Service {
    private static final String TAG = dataBaseService.class.getSimpleName();
    private StitchAppClient stitchClient;
    private RemoteMongoClient mongoClient;
    private RemoteMongoCollection sensorCollection;
    private final IBinder mBinder = new LocalBinder();
    public boolean init = false;
    private float distanceBound = 1260; // bounded radius of measurement
    private Location ucDavisCenter = new Location("");
    public class LocalBinder extends Binder {
        dataBaseService getService() {
            return dataBaseService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public boolean initialize() {
        if(init) return true;
        ucDavisCenter.setLatitude(38.5375166); // set center
        ucDavisCenter.setLongitude(-121.7574996);
        stitchClient = Stitch.getDefaultAppClient(); // establish stitchClient
        stitchClient.getAuth().loginWithCredential(new AnonymousCredential())
            .addOnCompleteListener(new OnCompleteListener<StitchUser>() {
                @Override
                public void onComplete(@NonNull Task<StitchUser> task) {
                    mongoClient = stitchClient.getServiceClient(RemoteMongoClient.factory, "mongodb-atlas");
                    sensorCollection = mongoClient.getDatabase(getResources().getString(R.string.data_base_name))
                            .getCollection(getResources().getString(R.string.collection_name));
                    init = true;
                }
            });
        return true;
    }

    public void addItem(dataSet data){ // convert data to BSON object
        if(data.getLoc() == null || sensorCollection == null) return;
        if(data.getLoc().distanceTo(ucDavisCenter) > distanceBound) return; // don't log data off of campus
        String currentDate = new SimpleDateFormat("ddMMyy", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("HHmmss", Locale.getDefault()).format(new Date());
        Document newItem = new Document()
        .append("PM10", 0)
        .append("PM25",data.getPm25())
        .append("PMTen", 0)
        .append("GPGLL", data.getLocationString())
        .append("TIME", currentTime)//Time = hhmmss.ss
        .append("DATE", currentDate);// Date = ddmmyy

        final Task<RemoteInsertOneResult> res = sensorCollection.insertOne(newItem);
        res.addOnCompleteListener(new OnCompleteListener<RemoteInsertOneResult>() {
            @Override
            public void onComplete(@NonNull final Task<RemoteInsertOneResult> task) {
                if (task.isSuccessful()) {
                    Log.println(Log.INFO, "success adding item","hurray");
                } else {
                    Log.e(TAG, "Error adding item", task.getException());
                }
            }
        });
    }
}