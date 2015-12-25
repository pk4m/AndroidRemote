package com.pandu.remotemouse;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ZoomControls;

public class TouchpadActivity extends Activity {

	private float x, y, oldX, oldY;
	private float dispX, dispY;

	private float x1, y1, x2, y2;
	private float oldx1, oldx2, oldy1, oldy2;
	private float olddistx, distx;
	private float dy1, dy2, dx1, dx2;

	private static final int TAP_NONE = 0;
	private static final int TAP_FIRST = 1;
	private static final int TAP_SECOND = 2;
	private static final int TAP_RIGHT = 5;

	private long lastTap = 0;
	private int tapState = TAP_NONE;
	private Timer tapTimer;

	private boolean isScrolling = false;
	private boolean valuesSet = false;

	private boolean oneFingerKeptBefore = false;
	private int pointerCount = 0;
	private int lastCount = 0;
	private RelativeLayout touchPad;
	private ImageButton showKeyboard;
	private EditText et;
	private InputMethodManager im;

	private DatagramSocket ds;
	private InetAddress addr;
	private int port;

	private void initializeVars(String ip) {
		try {
			addr = InetAddress.getByName(ip);
			port = getDefaultPort();
		} catch (UnknownHostException e) {
			String problem = "There was a problem establishing connection";
			Toast.makeText(TouchpadActivity.this, problem, Toast.LENGTH_LONG).show();
		}
	}

	private int getDefaultPort() {
		int defaultPort = 8811;
		return defaultPort;
	}

	private void initializeControls() {
		touchPad = (RelativeLayout) findViewById(R.id.flTouchPad);
		touchPad.setOnTouchListener(new MyOnTouchListener());

		final ZoomControls zoom = (ZoomControls) findViewById(R.id.zoomControls1);
		zoom.setOnZoomInClickListener(new OnClickListener() {

			public void onClick(View v) {
				sendData("keydown VK_CONTROL");
				sendData("scroll 6.0");
				sendData("keyup VK_CONTROL");
			}
		});
		zoom.setOnZoomOutClickListener(new OnClickListener() {

			public void onClick(View v) {
				sendData("keydown VK_CONTROL");
				sendData("scroll -6.0");
				sendData("keyup VK_CONTROL");
			}
		});

		et = (EditText) findViewById(R.id.editText1);
		et.setFocusable(true);
		et.setFocusableInTouchMode(true);
		et.setOnKeyListener(new MyKeyListener());
		et.addTextChangedListener(new MyKeyListener());

		showKeyboard = (ImageButton) findViewById(R.id.bShowKeyboard);
		showKeyboard.setOnClickListener(new ShowKeyboardListener());
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle gotBundle = getIntent().getExtras();
		String ip = gotBundle.getString("IP");
		initializeVars(ip);
		setContentView(R.layout.touchpad);
		initializeControls();

	}

	@Override
	protected void onPause() {
		super.onPause();
		try {
			ds.close();
		} catch (Exception e) {
			String problem = "There was a problem closing the connection";
			Toast.makeText(TouchpadActivity.this, problem, Toast.LENGTH_LONG).show();
		}

		try {
			finalize();
		} catch (Throwable e) {
			// do nothing
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		try {
			ds = new DatagramSocket();
		} catch (SocketException e) {
			String problem = "There was a problem establishing connection";
			Toast.makeText(TouchpadActivity.this, problem, Toast.LENGTH_LONG).show();
		}
	}

	public void sendData(String dataString) {
		byte[] data = dataString.getBytes();
		try {
			ds.send(new DatagramPacket(data, data.length, addr, port));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private class ShowKeyboardListener implements OnClickListener {
		public void onClick(View v) {
			et.requestFocus();
			im = (InputMethodManager) TouchpadActivity.this.getApplicationContext()
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			im.showSoftInput(et, 0);
		}

	}

	private class MyOnTouchListener implements OnTouchListener {

		public boolean onTouch(View v, MotionEvent event) {
			pointerCount = event.getPointerCount();

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (pointerCount == 1) {
					if (tapState == TAP_NONE) {
						lastTap = System.currentTimeMillis();
						tapState = TAP_FIRST;
					} else if (tapState == TAP_FIRST) {
						long now = System.currentTimeMillis();
						long elapsed = now - lastTap;
						if (elapsed < 200) {
							tapState = TAP_SECOND;
							sendData("clickhold");

						} else {
							tapState = TAP_NONE;
						}
						lastTap = System.currentTimeMillis();
					}

					oldX = event.getX();
					oldY = event.getY();
				}

				break;

			case MotionEvent.ACTION_UP:
				if (pointerCount == 1) {

					if (tapState == TAP_FIRST) {
						long now = System.currentTimeMillis();
						long elapsed = now - lastTap;
						if (elapsed < 200) {
							tapTimer = new Timer();
							tapTimer.schedule(new TimerTask() {
								@Override
								public void run() {
									tapState = TAP_NONE;
									sendData("click");
								}
							}, 200);

							lastTap = System.currentTimeMillis();
						} else {
							tapState = TAP_NONE;
							lastTap = 0;
						}
					} else if (tapState == TAP_SECOND) {
						sendData("clickrelease");
						tapState = TAP_NONE;
						lastTap = 0;
					}

					else if (tapState == TAP_RIGHT) {
						long now = System.currentTimeMillis();
						long elapsed = now - lastTap;
						if (elapsed < 200) {
							sendData("rightclick");
						}
						tapState = TAP_NONE;
					}
					// set to false when no finger on screen
					oneFingerKeptBefore = false;
					isScrolling = false;
					valuesSet = false;
				}
				break;

			case MotionEvent.ACTION_MOVE:
				if (pointerCount == 1 && tapState != TAP_RIGHT && !isScrolling) {
					if (tapState == TAP_SECOND) {
						long now = System.currentTimeMillis();
						long elapsed = now - lastTap;
						if (elapsed > 50) {
							tapTimer.cancel();
							lastTap = 0;
						}
					}

					if (lastCount != 1) {
						oldX = event.getX();
						oldY = event.getY();
					}

					x = event.getX();
					y = event.getY();
					dispX = x - oldX;
					dispY = y - oldY;
					oldX = x;
					oldY = y;
					oneFingerKeptBefore = true;
					sendData("moved " + dispX + " " + dispY);
				} else if (pointerCount == 2 && tapState != TAP_RIGHT
						&& !oneFingerKeptBefore) {
					// oneFingerKeptBefore variable used to see that one finger
					// is not
					// kept previously and second kept later
					tapState = TAP_RIGHT;
					lastTap = System.currentTimeMillis();
				} else if (pointerCount == 2 && !oneFingerKeptBefore) {
					if (!valuesSet) {
						oldx1 = event.getX(0);
						oldy1 = event.getY(0);
						oldx2 = event.getX(1);
						oldy2 = event.getY(1);
						olddistx = Math.abs(oldx1 - oldx2);
						valuesSet = true;
						break;
					}

					x1 = event.getX(0);
					y1 = event.getY(0);
					x2 = event.getX(1);
					y2 = event.getY(1);
					distx = Math.abs(x1 - x2);

					dx1 = oldx1 - x1;
					dx2 = oldx2 - x2;
					dy1 = oldy1 - y1;
					dy2 = oldy2 - y2;
					// ensure dx1, dx2, dy1, dy2 are not 0
					if (dx1 == 0) {
						dx1 = 0.01f;
					}
					if (dx2 == 0) {
						dx2 = 0.01f;
					}

					float slope1 = Math.abs(dy1 / dx1);
					float slope2 = Math.abs(dy2 / dx2);

					// scrolling
					if (Math.abs(olddistx - distx) < 10 && slope1 > 1
							&& slope2 > 1) {
						isScrolling = true;
						if (dy1 > 0 && dy2 > 0)
							sendData("scroll " + (dy1 + dy2) / 2);
						else if (dy1 < 0 && dy2 < 0)
							sendData("scroll " + (dy1 + dy2) / 2);
					}

					oldx1 = x1;
					oldy1 = y1;
					oldx2 = x2;
					oldy2 = y2;
					olddistx = Math.abs(oldx1 - oldx2);

				} else if (isScrolling && pointerCount == 1) {
					x1 = event.getX();
					y1 = event.getY();
					dx1 = oldx1 - x1;
					dy1 = oldy1 - y1;
					if (dx1 == 0) {
						dx1 = 0.01f;
					}
					float slope1 = Math.abs(dy1 / dx1);
					if (slope1 > 1) {
						if (dy1 > 00)
							sendData("scroll " + dy1);
						else if (dy1 < 0)
							sendData("scroll " + dy1);
					}

					oldx1 = x1;
					oldy1 = y1;

				}
				break;
			}
			// sending the state for debugging purpose
			// sendData(Integer.toString(tapState));

			lastCount = pointerCount;
			return true;
		}

	}

	public class MyKeyListener implements OnKeyListener, TextWatcher {

		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				switch (keyCode) {
				case KeyEvent.KEYCODE_DEL:
					sendData("keyin VK_BACKSPACE");
					break;
				case KeyEvent.KEYCODE_VOLUME_UP:
					sendData("keyin VK_RIGHT");
					break;
				case KeyEvent.KEYCODE_VOLUME_DOWN:
					sendData("keyin VK_LEFT");
					break;
				}
			} else if (event.getAction() == KeyEvent.ACTION_UP) {
				switch (keyCode) {
				case KeyEvent.KEYCODE_BACK:
					finish();
					break;
				case KeyEvent.KEYCODE_0:
					sendData("keyin 0");
					break;
				case KeyEvent.KEYCODE_1:
					sendData("keyin 1");
					break;
				case KeyEvent.KEYCODE_2:
					sendData("keyin 2");
					break;
				case KeyEvent.KEYCODE_3:
					sendData("keyin 3");
					break;
				case KeyEvent.KEYCODE_4:
					sendData("keyin 4");
					break;
				case KeyEvent.KEYCODE_5:
					sendData("keyin 5");
					break;
				case KeyEvent.KEYCODE_6:
					sendData("keyin 6");
					break;
				case KeyEvent.KEYCODE_7:
					sendData("keyin 7");
					break;
				case KeyEvent.KEYCODE_8:
					sendData("keyin 8");
					break;
				case KeyEvent.KEYCODE_9:
					sendData("keyin 9");
					break;
				case KeyEvent.KEYCODE_TAB:
					sendData("keyin VK_TAB");
					break;
				case KeyEvent.KEYCODE_ENTER:
					sendData("keyin VK_ENTER");
					break;
				}
			}
			// TODO this if condition to be commented later
			/*if (event.getAction() == KeyEvent.ACTION_UP)
				Toast.makeText(Main.this, "got " + keyCode, Toast.LENGTH_SHORT)
						.show();*/
			return true;
		}

		public void afterTextChanged(Editable s) {

		}

		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {

		}

		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			try {
				String str = s.subSequence(start, start + count).toString();
				if (str.equals(" ")) {
					str = "VK_SPACE";
				}
				sendData("keyin " + str);
			} catch (Exception e) {
				// Toast.makeText(Main.this, e.toString() + " " + count,
				// Toast.LENGTH_LONG).show();
			}
		}
	}

}