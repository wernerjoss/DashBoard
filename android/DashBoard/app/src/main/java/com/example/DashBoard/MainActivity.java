/**
 *  @version 1.0 (25.03.2016)
 *  @author Werner Joss
 *	derived from http://project-greengiant.googlecode.com/svn/trunk/Blog/Android Arduino Bluetooth/Android/AndroidArduinoBluetooth/src/Android/Arduino/Bluetooth/BluetoothTest.java
 *	@License GNU GPL V 2.0 or later
 */

 package com.example.DashBoard;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final String TAG = "DashBoard";

	private TextView StatusText;
	//	private TextView DataText;
	private TextView Temperature;
	private TextView RPM;

	BluetoothAdapter mBluetoothAdapter;
	BluetoothSocket mmSocket;
	BluetoothDevice mmDevice;
	OutputStream mmOutputStream;
	InputStream mmInputStream;
	Thread workerThread;
	byte[] readBuffer;
	int readBufferPosition;
	int counter;
	volatile boolean stopWorker;
	private int Status;
	private float small = 15;
	private float large = 40;
	String DeviceName = "HC-05";    // expected Name of BT Device

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		// DONE: force Landscape even if Dev is configured for screen rotation
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		final Button openButton = (Button) findViewById(R.id.open);
		final Button closeButton = (Button) findViewById(R.id.close);
		StatusText = (TextView) findViewById(R.id.StatusText);
		Temperature = (TextView) findViewById(R.id.TempText);
		RPM = (TextView) findViewById(R.id.RpmText);
		final int DispIndex = 1;	// 1=Temperature, 2=RPM
		StatusText.setText("No BT Device opened");

		//Open Button
		openButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Status = try_connectBT(openButton, closeButton);
				if (Status < 0)	{
					openButton.setVisibility(View.VISIBLE);
					closeButton.setVisibility(View.INVISIBLE);
					Temperature.setTextSize(small);
					RPM.setTextSize(small);
				}
			}
		});

		//Close button
		closeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				try {
					closeBT();
					openButton.setVisibility(View.VISIBLE);
					closeButton.setVisibility(View.INVISIBLE);
				} catch (IOException ex) {
					StatusText.setText("Exception, failed to close BT Device: " + ex.getMessage());	// + " ex type: " + ex.getClass());
				}
			}
		});

		// startup defaults:
		openButton.setVisibility(View.INVISIBLE);
		closeButton.setVisibility(View.INVISIBLE);

		// try to open Default Device upon Startup
		Status = try_connectBT(openButton, closeButton);
	}

	int try_connectBT(Button openButton, Button closeButton) {
		try {
			Status = findBT(DeviceName);
			if (Status == 0) {
				Status = openBT();
				if (Status == 0) {
					closeButton.setVisibility(View.VISIBLE);
					openButton.setVisibility(View.INVISIBLE);
					Temperature.setTextSize(large);
					RPM.setTextSize(large);
				}
			}
			return Status;
		} catch (Exception ex) {
			//	StatusText.setText("Exception, failed to open BT Device: " + ex.getMessage());	// + " ex type: " + ex.getClass());
			StatusText.setText("could not open BT Device " + mmDevice.getName());
			openButton.setVisibility(View.VISIBLE);
		}
		return -1;
	}

	int findBT(String DeviceName) {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			StatusText.setText("No bluetooth adapter available");
			return -1;
		}

		// DONE: don't crash if BT is disabeled !
		if (!mBluetoothAdapter.isEnabled()) {
			StatusText.setText("Bluetooth is not enabeled");
			Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBluetooth, 0);
			return -2;
		}

		// OPTION: let user select Device from List of all Found Devices
		// discarded for now: just look for predifinded Device (HC-05)
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		if (pairedDevices == null) {
			StatusText.setText("no paired BT Devices found");
			return -3;
		}
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				if (device.getName().equals(DeviceName)) {
					mmDevice = device;
					break;
				}
			}
		}
		StatusText.setText("Bluetooth Device " + mmDevice.getName() + " Found");
		return 0;
	}

	int openBT() throws IOException {
		UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
		mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
		if (mmSocket == null) {
			StatusText.setText("could not create Socket to Device");
			return -1;
		}
		mmSocket.connect();
		mmOutputStream = mmSocket.getOutputStream();
		mmInputStream = mmSocket.getInputStream();
		//StringReader r = new StringReader()
		StatusText.setText("Bluetooth " + mmDevice.getName() + " opened");

		beginListenForData();
		return 0;
	}

	void beginListenForData() {
		final Handler handler = new Handler();
		final byte delimiter = 10; //This is the ASCII code for a newline character
		stopWorker = false;
		readBufferPosition = 0;
		readBuffer = new byte[1024];
		workerThread = new Thread(new Runnable() {
			public void run() {
				int DispIndex = 0;
				while (!Thread.currentThread().isInterrupted() && !stopWorker) {
					try {
						int bytesAvailable = mmInputStream.available();
						if (bytesAvailable > 0) {
							byte[] packetBytes = new byte[bytesAvailable];
							mmInputStream.read(packetBytes);
							for (int i = 0; i < bytesAvailable; i++) {
								byte b = packetBytes[i];
								if (b == delimiter) {
									byte[] encodedBytes = new byte[readBufferPosition];
									System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
									final String data = new String(encodedBytes, "US-ASCII");
									readBufferPosition = 0;
									String DispStr = data;
									if (data.startsWith("T:"))	{
										DispStr = data.substring(data.lastIndexOf(":") + 1).concat(" °C");
										DispIndex = 0;
									}
									if (data.startsWith("D:"))	{
										DispStr = data.substring(data.lastIndexOf(":") + 1).concat(" RPM");
										DispIndex = 1;
									}
									final String S = DispStr;
									switch (DispIndex)	{
										case 0:
											handler.post(new Runnable() {
												public void run() {
													Temperature.setText(S);
												}
											});
											break;
										case 1:
											handler.post(new Runnable() {
												public void run() {
													//	DataText.setText(data);
													RPM.setText(S);
												}
											});
											break;
										default:
											handler.post(new Runnable() {
												public void run() {
													Temperature.setText(S);
												}
											});
											break;
									}
								} else {
									readBuffer[readBufferPosition++] = b;
								}
							}
						}
					} catch (IOException ex) {
						stopWorker = true;
					}
				}
			}
		});

		workerThread.start();
	}

	void closeBT() throws IOException {
		stopWorker = true;
		mmOutputStream.close();
		mmInputStream.close();
		mmSocket.close();
		StatusText.setText("Bluetooth Closed");
		Temperature.setText(" ");
		RPM.setText(" ");
		RPM.setTextSize(small);
		Temperature.setTextSize(small);
	}
}