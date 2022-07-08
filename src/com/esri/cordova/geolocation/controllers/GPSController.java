/**
 * @author Andy Gup
 *
 * Copyright 2016 Esri
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.​
 */
package com.esri.cordova.geolocation.controllers;


import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.location.OnNmeaMessageListener;
import android.location.GpsStatus.NmeaListener;
import android.os.Bundle;
import android.os.Looper;
import android.os.Build;
import android.util.Log;

import com.esri.cordova.geolocation.model.InitStatus;
import com.esri.cordova.geolocation.utils.ErrorMessages;
import com.esri.cordova.geolocation.utils.GPSLocation;
import com.esri.cordova.geolocation.utils.JSONHelper;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;
import java.util.ArrayList;

public final class GPSController implements Runnable {

    private static LocationManager _locationManager = null;
    private static LocationListener _locationListenerGPSProvider = null;
    private static OnNmeaMessageListener _nmeaListener = null;
    private static GpsStatus.NmeaListener _nmeaStatusListener = null;
    
    private static CallbackContext _callbackContext; // Threadsafe
    private static CordovaInterface _cordova;

    private static long _minDistance = 0;
    private static long _minTime = 0;
    private static boolean _buffer = false;
    private static int _bufferSize = 0;
    private static boolean _returnCache = false;
    private static boolean _returnSatelliteData = false;
    private static boolean _returnNMEAData = false;
    private static boolean _returnLocationData = false;
    private static GPSLocation gpsloc = new GPSLocation();

    private static final String TAG = "GeolocationPlugin";
    private ArrayList<String> nmeaMessages = new ArrayList<String>();
    private ArrayList<String> parsingErrors = new ArrayList<String>();
    private ArrayList<String> parsedTypes = new ArrayList<String>();

    public GPSController(
            CordovaInterface cordova,
            CallbackContext callbackContext,
            long minDistance,
            long minTime,
            boolean returnCache,
            boolean returnSatelliteData,
            boolean returnNMEAData,
            boolean returnLocationData,
            boolean buffer,
            int bufferSize
    ){
        _cordova = cordova;
        _callbackContext = callbackContext;
        _minDistance = minDistance;
        _minTime = minTime;
        _returnCache = returnCache;
        _returnSatelliteData = returnSatelliteData;
        _buffer = buffer;
        _bufferSize = bufferSize;
        _returnNMEAData = returnNMEAData;
        _returnLocationData = returnLocationData;
    }

    public void run(){
        // Reference: http://developer.android.com/reference/android/os/Process.html#THREAD_PRIORITY_BACKGROUND
        // android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);

        // We are running a Looper to allow the Cordova CallbackContext to be passed within the Thread as a message.
        if(Looper.myLooper() == null){
            _locationManager = (LocationManager) _cordova.getActivity().getSystemService(Context.LOCATION_SERVICE);
            Looper.prepare();
            startLocation();
            Looper.loop();
        }
    }

    public void startLocation(){

        if(!Thread.currentThread().isInterrupted()){
            Log.i(TAG,"Available location providers: " + _locationManager.getAllProviders().toString());

            Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                    Log.e(TAG, "Failing gracefully after detecting an uncaught exception on GPSController thread. "
                        + throwable.getMessage());
                    sendCallback(PluginResult.Status.ERROR,
                            JSONHelper.errorJSON("TEST", "Failing gracefully after detecting an uncaught exception on GPSController thread. "+ thread.toString() + " - "
                                    + throwable.getMessage()+"Stacktrace: "+throwable.getStackTrace().toString()));

                    stopLocation();
                }
            });


            final InitStatus gpsListener = new InitStatus(); 
            gpsListener.success = true;//setLocationListenerGPSProvider();
            InitStatus nmeaListener = new InitStatus(); // setNMEAProvider();
            //final InitStatus nmeaListener = setNMEAProvider();
            //InitStatus nmeaListener = new InitStatus();
            if (_returnNMEAData) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    nmeaListener = setNMEAProvider();
                } else {
                    nmeaListener = setNMEAListener();
                }  
            }


            if(!gpsListener.success ||  !nmeaListener.success){
                if (!nmeaListener.success) {
	            	if (nmeaListener.exception == null) {
	                	sendCallback(PluginResult.Status.ERROR,
	                            JSONHelper.errorJSON("NMEA", nmeaListener.error));
	                }else {
	                    // Handle system exceptions
	                    sendCallback(PluginResult.Status.ERROR,
	                            JSONHelper.errorJSON("NMEA", nmeaListener.exception));
	                }
                }
                if (!gpsListener.success) {
	            	if(gpsListener.exception == null){
	                    // Handle custom error messages
	                    sendCallback(PluginResult.Status.ERROR,
	                            JSONHelper.errorJSON(LocationManager.GPS_PROVIDER, gpsListener.error));
	                }
	                else {
	                    // Handle system exceptions
	                    sendCallback(PluginResult.Status.ERROR,
	                            JSONHelper.errorJSON(LocationManager.GPS_PROVIDER, gpsListener.exception));
	                }
                }
            }
            else {
                
                
            }
        }
        else {
            Log.e(TAG, "Not starting GPSController due to thread interrupt.");
        }
    }

    /**
     * Full stop using brute force. Works with many Android versions.
     */
    public void stopLocation(){

        if(_locationManager != null){
            Log.d(TAG, "Attempting to stop gps geolocation");

            if(_locationListenerGPSProvider != null){

                try {
                    _locationManager.removeUpdates(_locationListenerGPSProvider);
                }
                catch(SecurityException exc){
                    Log.e(TAG, exc.getMessage());
                }

                _locationListenerGPSProvider = null;
            }

            if(_nmeaListener != null){

                try {
                    _locationManager.removeNmeaListener(_nmeaListener);
                }
                catch(SecurityException exc){
                    Log.e(TAG, exc.getMessage());
                }

                _nmeaListener = null;
            }

            _locationManager = null;


            try {
                Thread.currentThread().interrupt();
            }
            catch(SecurityException exc){
                Log.e(TAG, exc.getMessage());
                sendCallback(PluginResult.Status.ERROR,
                        JSONHelper.errorJSON(LocationManager.GPS_PROVIDER, ErrorMessages.FAILED_THREAD_INTERRUPT()));
            }
        }
        else{
            Log.d(TAG, "GPS location already stopped");
        }
    }

    /**
     * Callback handler for this Class
     * @param status Message status
     * @param message Any message
     */
    private static void sendCallback(PluginResult.Status status, String message){
        if(!Thread.currentThread().isInterrupted()){
            final PluginResult result = new PluginResult(status, message);
            result.setKeepCallback(true);
            _callbackContext.sendPluginResult(result);
        }
    }


    /* Für Android 6 */
    private InitStatus setNMEAProvider(){
    	final InitStatus status = new InitStatus();
        try {
        	_nmeaListener = new OnNmeaMessageListener() {

        		public void onNmeaMessage(String message, long timestamp) {
        		
                //if(!Thread.currentThread().isInterrupted()){
                    try {
                        /* Adding Sentences to Object */
                        gpsloc.addSentence(message);
                        /* Parsing NMEA Data to Object */
                        if (gpsloc.getUTC(message)!=null) {
                            if(!gpsloc.checkUTC(gpsloc.getUTC(message))) {
                                /* Auswerten des Objektes und zurücksenden! */
                                String loc = gpsloc.getLocation(parsingErrors, parsedTypes);
                                if (loc != null) {
                                    sendCallback(PluginResult.Status.OK,
                                        JSONHelper.nmeaJSON("NMEA", loc, timestamp));
                                }
                            //	parsingErrors = new ArrayList<String>();
                            //	parsedTypes = new ArrayList<String>();
                                gpsloc.clear();
                            }
                        }
                        /* Gehört noch zur Serie */
                        String mt = null;
                        try {
                            mt = gpsloc.messageType(message);
                            //parsedTypes.add(mt);
                        } catch (Exception exc) {
                            sendCallback(PluginResult.Status.ERROR,
                                    JSONHelper.errorJSON("NMEA", "Could not get Message type"
                                + exc.getMessage() + "- "+message));
                        }

                        try {
                            if (mt != null && !mt.isEmpty()) {
                                mt = mt.toUpperCase();
                                //parsedTypes.add(mt);
                                if (mt.equalsIgnoreCase("GST")) {
                                        gpsloc.parseGST(message);
                                        if (gpsloc.parseError()) {
                                            parsingErrors.add(gpsloc.getError());
                                        }
                                } else if (mt.equalsIgnoreCase("GGA")) {
                                        gpsloc.parseGGA(message);
                                        if (gpsloc.parseError()) {
                                            parsingErrors.add(gpsloc.getError());
                                        }
                                } else if (mt.equalsIgnoreCase("VTG")) {
                                        gpsloc.parseVTG(message);
                                        if (gpsloc.parseError()) {
                                            parsingErrors.add(gpsloc.getError());
                                        }
                                } else if (mt.equalsIgnoreCase("ZDA")) {
                                        gpsloc.parseZDA(message);
                                        if (gpsloc.parseError()) {
                                            parsingErrors.add(gpsloc.getError());
                                        }
                                } else if (mt.equalsIgnoreCase("GSA")) {
                                        gpsloc.parseGSA(message);
                                        if (gpsloc.parseError()) {
                                            parsingErrors.add(gpsloc.getError());
                                        }
                                }
                            }
                        } catch (Exception exc) {
                            sendCallback(PluginResult.Status.ERROR,
                                    JSONHelper.errorJSON("NMEA", "Could not parse"
                                + exc.getMessage() + "- "+message));
                        }


                    } catch (Exception exc) {
                        sendCallback(PluginResult.Status.ERROR,
                                JSONHelper.errorJSON("NMEA", "Meine Ausgabe - vielleicht mehr info"
                                        + exc.getMessage()));
                    }
                };
        	};
        } catch (Exception ex) {
        	status.success = false;
        	status.exception = ex.getMessage();
        }

        final Boolean gpsProviderEnabled = _locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if(gpsProviderEnabled){
        	try{
                Log.d(TAG, "Starting NMEA");
                // Register the listener with the Location Manager to receive location updates
                _locationManager.addNmeaListener(_nmeaListener);
            }
            catch(SecurityException exc){
                Log.e(TAG, "Unable to start NMEA listener. " + exc.getMessage());
                status.success = false;
                status.exception = exc.getMessage();
            }
        } else {
        	status.success = false;
            status.error = ErrorMessages.GPS_UNAVAILABLE();
        }

        return status;
	}




    private InitStatus setLocationListenerGPSProvider(){
        _locationListenerGPSProvider = new LocationListener() {

            public void onLocationChanged(Location location) {
                if (_returnLocationData) {
	                if(_buffer && !Thread.currentThread().isInterrupted()){
	                    final Coordinate coordinate = new Coordinate();
	                    coordinate.latitude = location.getLatitude();
	                    coordinate.longitude = location.getLongitude();
	                    coordinate.accuracy = location.getAccuracy();

	                    // Get the size of the buffer
	                    final int size = _locationDataBuffer.add(coordinate);

	                    final Coordinate center = _locationDataBuffer.getGeographicCenter();
	                    sendCallback(PluginResult.Status.OK,
	                            "GEOOBJEKT"
	                    );

	                }
	                else {
	                    sendCallback(PluginResult.Status.OK,
	                            JSONHelper.locationJSON(LocationManager.GPS_PROVIDER, location, false));
	                }
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                switch (status) {
                    case LocationProvider.OUT_OF_SERVICE:
                        // Reference: https://developer.android.com/reference/android/location/LocationProvider.html#OUT_OF_SERVICE
                        Log.d(TAG, "Location Status Changed: " + ErrorMessages.GPS_OUT_OF_SERVICE().message);
                        sendCallback(PluginResult.Status.ERROR,
                                JSONHelper.errorJSON(LocationManager.GPS_PROVIDER, ErrorMessages.GPS_OUT_OF_SERVICE()));

                        break;
                    case LocationProvider.TEMPORARILY_UNAVAILABLE:
                        Log.d(TAG, "Location Status Changed: " + ErrorMessages.GPS_UNAVAILABLE().message);
                        sendCallback(PluginResult.Status.ERROR,
                                JSONHelper.errorJSON(LocationManager.GPS_PROVIDER, ErrorMessages.GPS_UNAVAILABLE()));
                        break;
                    case LocationProvider.AVAILABLE:
                        Log.d(TAG, "Location Status Changed: GPS Available");
                        break;
                }
            }

            public void onProviderEnabled(String provider) {
                startLocation();
            }

            public void onProviderDisabled(String provider) {
                stopLocation();
            }
        };

        final InitStatus status = new InitStatus();
        final Boolean gpsProviderEnabled = _locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if(gpsProviderEnabled){

            try{
                Log.d(TAG, "Starting LocationManager.GPS_PROVIDER");
                // Register the listener with the Location Manager to receive location updates
                _locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, _minTime, _minDistance, _locationListenerGPSProvider);
            }
            catch(SecurityException exc){
                Log.e(TAG, "Unable to start GPS provider. " + exc.getMessage());
                status.success = false;
                status.exception = exc.getMessage();
            }
        }
        else {
            Log.w(TAG, ErrorMessages.GPS_UNAVAILABLE().message);
            //GPS not enabled
            status.success = false;
            status.error = ErrorMessages.GPS_UNAVAILABLE();
        }

        return status;
    }
}
