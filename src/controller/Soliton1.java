package controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.util.Log;
import autorad.android.C;
import autorad.android.sensor.DataType;
import autorad.android.sensor.SensorDataListener;
import autorad.android.sensor.SensorDataSource;
import autorad.android.transport.DataListener;
import autorad.android.transport.DataStatusChangeListener;
import autorad.android.transport.Soliton1UdpReceiver;

public class Soliton1 implements SensorDataSource, DataListener {

	 
	
	private Soliton1UdpReceiver receiver;
	private DataStatusChangeListener statusListener;
	
	private ArrayList<SensorDataListener> listeners = new ArrayList<SensorDataListener>();
	private Lock lock = new ReentrantLock();
	
	BufferedWriter writer;
	
	public Soliton1() {
		
	}
	
	public void registerSensorDataListener(SensorDataListener listener) {
		try {
			lock.lock();
			listeners.add(listener);
		} finally {
			lock.unlock();
		}
	}
	
	public void unregisterSensorDataListener(SensorDataListener listener) {
		try {
			lock.lock();
			listeners.remove(listener);
		} finally {
			lock.unlock();
		}
	}
	
	
	public void start() {
		if (receiver == null) {
			receiver = new Soliton1UdpReceiver(this);
		}
		receiver.setDataStatusChangeListener(statusListener);
		(new Thread(receiver)).start();
	}
	
	public void registerDataStatusChangeListener(DataStatusChangeListener listener) {
		this.statusListener = listener;
		if (receiver != null) {
			receiver.setDataStatusChangeListener(statusListener);
		}
	}
		
	public void stop() {
		statusListener = null;
		receiver.setDataStatusChangeListener(null);
		receiver.finish();
		stopDataLogging();
		
		try {
			if (lock != null) {
				lock.lock();
			}
			if (listeners != null) {
				listeners.clear();
			}
		} finally {
			if (lock != null) {
				lock.unlock();
			}
		}
		lock = null;
		
	}

	public void startDataLogging() {
		if (writer == null) {
			File file = new File("/sdcard/soliton1.log");
	        try {
	        	if (!file.exists()) {
	        		file.createNewFile();
	        		Log.e(C.TAG, "File Created!");
	        	}
	        	
	        	writer = new BufferedWriter(new FileWriter(file));
	        }
	        catch (IOException e) {
	        	Log.e(C.TAG, e.toString());
	        } 
		}
		
	}
	
	public void stopDataLogging() {
		
       	try {
	       	if (writer != null) {
	       		writer.flush();
	       		writer.close();
	        }
       	} catch (IOException e) {
       		Log.e(C.TAG, e.toString());
       	}

		
	}
	
	private static byte LOG_MSTICS = 1;
	private static byte LOG_MINTICS = 2;
	private static byte LOG_AUXV = 3;
	private static byte LOG_PACKV = 4;
	private static byte LOG_CURRENT = 5;
	private static byte LOG_TEMP = 6;
	private static byte LOG_INPUT3 = 7;
	private static byte LOG_INPUT2 = 8;
	private static byte LOG_INPUT1 = 9;
	private static byte LOG_THROTTLE = 10;
	private static byte LOG_CPULOAD = 11;
	private static byte LOG_PWM = 12;
	private static byte LOG_RPM = 13;
	private static byte LOG_RPMERROR = 14;
	private static byte LOG_NUMFIELDS = 15;

	
	public void onData(int[] data) {

		//int timestamp = data[LOG_MSTICS] + data[LOG_MINTICS] * 60000;
		//int cpuLoad = (data[LOG_CPULOAD]*100)/128;
		//int pwm = data[LOG_PWM]/10;
		//int temp = data[LOG_TEMP]/10;
		try {
		
			lock.lock();
		
			if (writer != null) {
				StringBuilder builder = new StringBuilder();
				builder.append(data[0]);
				for (int i=1; i<data.length; i++) {
					builder.append(",").append(data[i]);
				}
				try {
					writer.write(builder.toString());
					writer.newLine();
				} catch (IOException e) {
					Log.e(C.TAG, "Error writing log file: " + e.getMessage());
				}
			}
			
			for (SensorDataListener listener : listeners) {
		
				for (DataType dt :  listener.getDataTypes())
				switch (dt) {
				
				case CURRENT:
					listener.onData(DataType.CURRENT, data[LOG_CURRENT]);
					break;
		
				case TEMPERATURE_CELCIUS:
					listener.onData(DataType.TEMPERATURE_CELCIUS, data[LOG_TEMP]/10);
					break;
					
				case VOLTAGE:
					listener.onData(DataType.VOLTAGE, data[LOG_PACKV]);
					break;
					
				case RPM:
					listener.onData(DataType.RPM, data[LOG_RPM]);
					break;
		
				}
			}
		} finally {
			lock.unlock();
		}
	}
	
	

	
}
