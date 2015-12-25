package com.pandu.remotemouse;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Main extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		final EditText ip = (EditText) findViewById(R.id.etIpAddress);
		Button b = (Button) findViewById(R.id.bConnect);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String ipAddress = ip.getText().toString();
				if (isIpValid(ipAddress)) {					
					Bundle bundle = new Bundle();
					bundle.putString("IP", ipAddress);
					Intent i = new Intent(Main.this, TouchpadActivity.class);
					i.putExtras(bundle);
					startActivity(i);
				}else{
					Toast.makeText(Main.this, "Invalid IP", Toast.LENGTH_SHORT).show();
				}
			}

			private boolean isIpValid(String ip) {
				if (ip.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")) {
					String[] ipParts = ip.split(".");
					for (String s : ipParts) {
						int i = Integer.parseInt(s);
						if (i > 255) {
							return false;
						}
					}
				} else {
					return false;
				}
				return true;
			}
		});
	}

}
