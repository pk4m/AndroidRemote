package com.pandu.remotemouse;

import java.util.LinkedList;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class Main extends Activity {

	String filename = "Remote Mouse Keyboard";
	SharedPreferences prefs;
	int MAX_HOSTS = 4;
	LinkedList<String> ipSet = new LinkedList<String>();
	ListView lv;
	EditText editTextIp;

	private void addIpToList(String ipAddress) {
		if (ipSet.contains(ipAddress)) {
			ipSet.remove(ipAddress);
		} else if (ipSet.size() == MAX_HOSTS) {
			ipSet.remove(MAX_HOSTS - 1);
		}
		ipSet.add(0, ipAddress);
		Editor e = prefs.edit();
		e.clear();
		for (int i = 0; i < ipSet.size(); i++) {
			e.putString("IP" + i, ipSet.get(i));
		}
		e.commit();
	}

	private String[] getIpsFromList() {
		String[] retStringArray = new String[ipSet.size()];
		for(int i = 0; i < ipSet.size(); i++){
			retStringArray[i] = ipSet.get(i);
		}
		return retStringArray;
	}
	
	private void setListValues(){
		for (int i = 0; i < MAX_HOSTS; i++) {
			if (prefs.contains("IP" + i)) {
				ipSet.add(prefs.getString("IP" + i, "value not set"));
				Log.d("IP" + i, prefs.getString("IP" + i, "value not set"));
			}
		}
		if (ipSet.size() > 0) {
			String[] listIps = getIpsFromList();
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_1, listIps);
			lv.setAdapter(adapter);
		}
	}
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		prefs = getSharedPreferences(filename, 0);
		lv = (ListView) findViewById(R.id.lvIpAddresses);
		setListValues();
		lv.setOnItemClickListener(new MyItemListener());
		
		editTextIp = (EditText) findViewById(R.id.etIpAddress);

		Button b = (Button) findViewById(R.id.bConnect);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String ipAddress = editTextIp.getText().toString();
				if (isIpValid(ipAddress)) {
					addIpToList(ipAddress);
					Bundle bundle = new Bundle();
					bundle.putString("IP", ipAddress);
					Intent i = new Intent(Main.this, TouchpadActivity.class);
					i.putExtras(bundle);
					startActivity(i);
				} else {
					Toast.makeText(Main.this, "Invalid IP", Toast.LENGTH_SHORT)
							.show();
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

		Button help = (Button) findViewById(R.id.bHelp);
		help.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				// TODO Auto-generated method stub
				Dialog d = new Help(Main.this);
				d.show();
			}
		});

	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		finish();
	}

	

	
	private class MyItemListener implements OnItemClickListener{

		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// TODO Auto-generated method stub
			String ipAddress = ipSet.get(position);
			editTextIp.setText(ipAddress);
		}
		
	}

}
