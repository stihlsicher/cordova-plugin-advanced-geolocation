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
package com.esri.cordova.geolocation.utils;

import android.location.GpsSatellite;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.SignalStrength;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import com.esri.cordova.geolocation.model.Error;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.time.*;
import java.lang.*;

/**
 * Threadsafe class for converting location data into JSON
 */
public class GPSLocation {

	private long timestamp;
	private float latitude;
	private float longitude;
	private int quality;
	private float accuracy;
	private float speed;
	private float bearing;
	private float altitude;
	private float altitude_accuracy;
	private float rtk_accuracy;
	private float rtk_altitude_accuracy;
	private float pdop;
	private float hdop;
	private float vdop;
	private int fixtype;
	private String utc;
	
	public GPSLocation() {
		
	}
	
	public void clear() {
		timestamp = 0;
		latitude = 0;
		longitude = 0;
		quality = 0;
		accuracy = 0;
		speed = 0;
		bearing = 0;
		altitude = 0;
		altitude_accuracy = 0;
		rtk_accuracy = 0;
		rtk_altitude_accuracy = 0;
		pdop = 0;
		hdop = 0;
		vdop = 0;
		fixtype = 1;
		utc = "";
	}
	
	public boolean checkUTC(String utc_chk) {
		return this.utc.equalsIgnoreCase(utc_chk);
	}
	
	public String messageType(String message) {
		if (message.subString(0,2).equalsIgnoreCase("$G")) {
			return message.subString(3,6);
		} else {
			return "NONE";
		}
	}
	
	public String getUTC(String message) {
		String mt = this.messageType(message);
		if (mt.contentEquals("GST") ||mt.contentEquals("GGA") || mt.contentEquals("RMC")) {
			String[] mp = message.split(",");
			return mp[1];
		}
		return null;
	}
	
	public String getLocation() {
    	final JSONObject json = new JSONObject();
    	try {
    		if (this.quality == 0) {
    			return null;
    		}
    		if (this.quality < 3) {
    			json.put("provider","gps");
    		}
    		if (this.qualtiy > 3) {
    			json.put("provider","rtk");
    		}
    		json.put("timestamp",this.timestamp);
    		json.put("latitude",this.latitude);
    		json.put("longitude",this.longitude);
    		json.put("quality",this.quality);
    		json.put("accuracy",this.accuracy);
    		json.put("speed",this.speed);
    		json.put("bearing",this.bearing);
    		json.put("altitude",this.altitude);
    		json.put("altitude_accuracy",this.altitude_accuracy);
    		json.put("rtk_accuracy",this.rtk_accuracy);
    		json.put("rtk_altitude_accuracy",this.rtk_altitude_accuracy);
    		json.put("vdop",this.vdop);
    		json.put("hdop",this.hdop);
    		json.put("pdop",this.pdop);
    		json.put("fixtype",this.fixtype);
    	} catch (JSONException exc) {
            logJSONException(exc);
        }
    	return json.toString();
    }
	
	
	
	public void parseGGA(String message) {
		String[] mp = message.split(",");
		/* Parsing time if not already set */
		if (this.timestamp == null) {
			int len = mp[1].length();
			d = mp[1].indexOf(".");
			hcount = d - 4;
			String h = mp[1].subString(0,hcount);
			String m = mp[1].subString(hcount,hcount+2);
			String s = mp[1].subString(hcount+2,hcount+4);
			String t = mp[1].subString(d+1);
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd"); 
			String datestr = formatter.format(currentTime);
			String timestr = datestr+"T"+h+":"+m+":"+s+"Z";
			Instant instant = Instant.parse( timestr );
			this.timestamp = instant.toEpochMilli();
		}
		this.utc = mp[1];
		this.quality = parseInt(mp[6]);
		if (this.quality > 0) {
			
			/* Parsing Latitude */
			String lat_deg = mp[2].subString(0, 2);
			String lat_min1 = mp[2].subString(2, 4);
			String lat_min2 = mp[2].subString(5);
			String lat_min3 = "0." + lat_min1 + lat_min2;
			float lat_dec = Float.parseFloat(lat_min3)/.6f;
			this.latitude = Float.parseFloat(lat_deg) + lat_dec;

			// Direction of latitude. North is positive, south negative
			if (mp[3].equals("N")) {
			  // no correction needed
			} else {
			  this.longitude = this.latitude * -1;
			}
			
			/* Parsing longitude */
			String lon_deg = mp[4].subString(0, 3);
			String lon_min1 = mp[4].subString(3, 5);
			String lon_min2 = mp[4].subString(6);
			String lon_min3 = "0." + lon_min1 + lon_min2;
			float lon_dec = Float.parseFloat(lon_min3)/.6f;
			this.longitude = Float.parseFloat(lon_deg) + lon_dec;
			//direction of longitude, east is positive
			if (mp[5].equals("E")) {
			    // No correction needed
			} else {
			  this.longitude = this.longitude * -1;
			}
			
			this.hdop = Float.parseFloat(mp[8]);
			this.altitude = Float.parseFloat(mp[9]);
		}
	}
	
	public void parseGSA(String message) {
		String[] mp = message.split(",");
		this.fixtype = parseInt(mp[2]);
		int l = mp.length;
		this.pdop = Float.parseFloat(mp[l-3]);
		this.hdop = Float.parseFloat(mp[l-2]);
		String v = mp[l-1];
		String vh = v.subString(0,v.indexOf("*"));
		this.vdop = Float.parseFloat(v);
	}
	
	public void parseZDA(String message) {
		String[] mp = message.split(",");
		 int len = mp[1].length();
		 int d = mp[1].indexOf(".");
		 int hcount = d - 4;
		 String h = mp[1].subString(0,hcount);
		 String m = mp[1].subString(hcount,hcount+2);
		 String s = mp[1].subString(hcount+2,hcount+4);
		 String t = mp[1].subString(d+1);
		String timestr = mp[4]+"-"+mp[3]+"-"+mp[2]+"T"+h+":"+m+":"+s+"Z";
		Instant instant = Instant.parse( timestr );
		this.timestamp = instant.toEpochMilli();
	}
	
    public void parseVTG(String message) {
    	String[] mp = message.split(",");
    	this.bearing = Float.parseFloat(mp[1]);
    	this.speed = Float.parseFloat(mp[7]);
    	this.speed = this.speed / 3.6;
    }
    
    public void parseGST(String message) {
    	String[] mp = message.split(",");
		this.utc = mp[1];
    	this.rtk_accuracy = Float.parseFloat(mp[6]);
    	this.rtk_altitude_accuracy = Float.parseFloat(mp[8]);
    }
	
    
	
}
