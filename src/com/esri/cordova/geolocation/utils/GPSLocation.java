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
import java.lang.*;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Threadsafe class for converting location data into JSON
 */
public class GPSLocation {

	private long timestamp;
	private Float latitude;
	private Float longitude;
	private int quality;
	private Float accuracy;
	private Float speed;
	private Float bearing;
	private Float altitude;
	private Float altitude_accuracy;
	private Float rtk_accuracy;
	private Float rtk_altitude_accuracy;
	private Float pdop;
	private Float hdop;
	private Float vdop;
	private int fixtype;
	private String utc;
	private String errorMessage;
	private boolean error = false;
	private ArrayList<String> mtypes = new ArrayList<String>();
	
	public GPSLocation() {
		this.clear();
	}
	
	public void clear() {
		this.timestamp = 0;
		this.latitude = null;
		this.longitude = null;
		this.quality = 0;
		this.accuracy = null;
		this.speed = null;
		this.bearing = null;
		this.altitude = null;
		this.altitude_accuracy = null;
		this.rtk_accuracy = null;
		this.rtk_altitude_accuracy = null;
		this.pdop = null;
		this.hdop = null;
		this.vdop = null;
		this.fixtype = 1;
		this.utc = null;
		this.errorMessage = null;
		this.error = false;
	}
	
	public boolean parseError() {
		return this.error;
	}
	
	public String getError() {
		String e = this.errorMessage;
		this.errorMessage = null;
		this.error = false;
		return e;
	}
	
	public boolean checkUTC(String utc_chk) {
		boolean test = (this.utc == null  || this.utc.isEmpty());
		boolean t2 = (utc_chk ==null);
		if (!test && !t2) {
			return this.utc.equalsIgnoreCase(utc_chk);
		} else {
			return true;
		}
	}
	
	public String messageType(String message) {
		//mtypes.add(message.substring(0,6));
		if (message.length() > 1 && message.substring(0,2).equalsIgnoreCase("$G")) {
			return message.substring(3,6);
		} else {
			return "NONE";
		}
	}
	
	public String getUTC(String message) {
		String mt = null;
		try {
			mt = this.messageType(message);
		} catch (Exception e) {
			this.error = true;
			this.errorMessage = e.getMessage();
		}
		try {
			if (mt != null && !mt.isEmpty()) {
				if (mt.contentEquals("GST") ||mt.contentEquals("GGA") || mt.contentEquals("RMC")) {
					String[] mp = message.split(",");
					return mp[1];
				}
			}
		} catch (Exception e) {
			this.error = true;
			this.errorMessage = e.getMessage();
		}
		return null;
	}
	
	public String getLocation(ArrayList<String> parsingErrors, ArrayList<String> parsedTypes) {
    	final JSONObject json = new JSONObject();
    	try {
    		if (this.quality == 0) {
    			//return null;
    			json.put("service","NO-FIX");
    		}
    		if (this.quality == 2) {
    			json.put("service","GPS");
    		}
    		if (this.quality == 3) {
    			json.put("service","RTD");
    		}
    		if (this.quality == 4) {
    			json.put("service","RTK fix");
    		}
    		if (this.quality == 5) {
    			json.put("service","RTK float");
    		}
    	//	json.put("parsingErrors",parsingErrors);
    //		json.put("parsedTypes",parsedTypes);
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
    	//	json.put("messageTypes",this.mtypes);
    		
    	} catch (Exception exc) {
            return "Fehler getLocation: "+exc.getMessage();
        }
    	return json.toString();
    }
	
	
	
	public void parseGGA(String message) {
		try {
			String[] mp = message.split(",");
			/* Parsing time if not already set */
			if (this.timestamp == 0 && !mp[1].isEmpty()) {
				int len = mp[1].length();
				int d = mp[1].indexOf(".");
				int hcount = d - 4;
				String mt = mp[1];
				int h = Integer.parseInt(mt.substring(0,hcount));
				int m = Integer.parseInt(mt.substring(hcount,hcount+2));
				int s = Integer.parseInt(mt.substring(hcount+2,hcount+4));
				String t = mp[1].substring(d+1);
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd"); 
				Date currentTime = new Date();
				this.timestamp =  Date.UTC(currentTime.getYear(), currentTime.getMonth(), currentTime.getDate(), h, m, s);
				//Calendar calendar = Calendar.getInstance();
				//calendar.setTime(result);
				//this.timestamp = calendar.getTimeInMillis();
			}
			if (!mp[1].isEmpty()) {
				this.utc = mp[1];
			}
			if (!mp[6].isEmpty()) {
				this.quality = Integer.parseInt(mp[6]);
			}
			if (this.quality > 0) {
				if (!mp[2].isEmpty()) {
					/* Parsing Latitude */
					String lat_deg = mp[2].substring(0, 2);
					String lat_min1 = mp[2].substring(2, 4);
					String lat_min2 = mp[2].substring(5);
					String lat_min3 = "0." + lat_min1 + lat_min2;
					Float lat_dec = Float.parseFloat(lat_min3)/.6f;
					this.latitude = Float.parseFloat(lat_deg) + lat_dec;
					// Direction of latitude. North is positive, south negative
					if (!mp[3].isEmpty() && mp[3].equals("N")) {
					  // no correction needed
					} else {
					  this.longitude = this.latitude * -1;
					}

				}
	
				if (!mp[4].isEmpty()) {	
					/* Parsing longitude */
					String lon_deg = mp[4].substring(0, 3);
					String lon_min1 = mp[4].substring(3, 5);
					String lon_min2 = mp[4].substring(6);
					String lon_min3 = "0." + lon_min1 + lon_min2;
					Float lon_dec = Float.parseFloat(lon_min3)/.6f;
					this.longitude = Float.parseFloat(lon_deg) + lon_dec;
					//direction of longitude, east is positive
					if (!mp[5].isEmpty() && mp[5].equals("E")) {
					    // No correction needed
					} else {
					  this.longitude = this.longitude * -1;
					}
				}
				if (!mp[8].isEmpty()) {
					this.hdop = Float.parseFloat(mp[8]);
					this.accuracy = Float.parseFloat(mp[8]);
				}
				if (!mp[9].isEmpty()) {
					this.altitude = Float.parseFloat(mp[9]);
				}
			}
		} catch (Exception exc) {
			this.error = true;
			this.errorMessage = exc.getMessage();
		}
	}
	
	public void parseGSA(String message) {
		try {
			String[] mp = message.split(",");
			if (!mp[2].isEmpty()) {
				this.fixtype = Integer.parseInt(mp[2]);
			}
			int l = mp.length;
			
			if (!mp[l-3].isEmpty()) {
				this.pdop = Float.parseFloat(mp[l-3]);
			}
			if (!mp[l-2].isEmpty()) {
				this.hdop = Float.parseFloat(mp[l-2]);
			}
			if (!mp[l-1].isEmpty()) {
				String v = mp[l-1];
				int astpos = v.indexOf("*");
				if (astpos > 0) {
					String vh = v.substring(0,astpos);
					this.vdop = Float.parseFloat(vh);
					this.altitude_accuracy = this.vdop;
				}
			}
		} catch (Exception exc) {
			this.error = true;
			this.errorMessage = exc.getMessage();
		}
	}
	
	public void parseZDA(String message) {
		try {
			String[] mp = message.split(",");
			if (!mp[1].isEmpty()) {
				int len = mp[1].length();
				int d = mp[1].indexOf(".");
				int hcount = d - 4;
				String mt = mp[1];
				int h = Integer.parseInt(mt.substring(0,hcount));
				int m = Integer.parseInt(mt.substring(hcount,hcount+2));
				int s = Integer.parseInt(mt.substring(hcount+2,hcount+4));
				String t = mt.substring(d+1);
				int year = Integer.parseInt(mp[4]);
				int month = Integer.parseInt(mp[3]);
				int day = Integer.parseInt(mp[2]);
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd"); 
				Date currentTime = new Date();
				this.timestamp =  Date.UTC(year, month, day, h, m, s);
			}
		} catch (Exception exc) {
			this.error = true;
			this.errorMessage = exc.getMessage();
		}
	}
	
    public void parseVTG(String message) {
    	try {
	    	String[] mp = message.split(",");
	    	if (!mp[1].isEmpty()) {
	    		this.bearing = Float.parseFloat(mp[1]);
	    	}
	    	if (!mp[7].isEmpty()) {
	    		this.speed = Float.parseFloat(mp[7]);
	    		this.speed = this.speed / (float)3.6;
	    	}
		} catch (Exception exc) {
			this.error = true;
			this.errorMessage = exc.getMessage();
		}
	}
    
    public void parseGST(String message) {
    	try {
	    	String[] mp = message.split(",");
	    	if (!mp[1].isEmpty()) {
	    		this.utc = mp[1];
	    	}
	    	if (!mp[6].isEmpty()) {
	    		this.rtk_accuracy = Float.parseFloat(mp[6]);
	    	}
	    	if (!mp[8].isEmpty()) {
	    		String mt = mp[8];
	    		int astpos = mt.indexOf("*");
	    		if (astpos > 0) {
	    			mt = mt.substring(0,astpos);
	    			this.rtk_altitude_accuracy = Float.parseFloat(mt);
	    		}
	    	}
		} catch (Exception exc) {
			this.error = true;
			this.errorMessage = exc.getMessage();
		}
    }
	
    
	
}
