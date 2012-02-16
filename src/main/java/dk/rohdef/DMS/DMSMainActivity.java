package dk.rohdef.DMS;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class DMSMainActivity extends Activity {

    private static String TAG = "DMS";

    /**
     * Called when the activity is first created.
     * @param savedInstanceState If the activity is being re-initialized after 
     * previously being shut down then this Bundle contains the data it most 
     * recently supplied in onSaveInstanceState(Bundle). <b>Note: Otherwise it is null.</b>
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
        setContentView(R.layout.main);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflator = getMenuInflater();
    	inflator.inflate(R.menu.mainmenu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.preferences:
    		Intent i = new Intent(DMSMainActivity.this, DMSPreferencesActivity.class);
    		startActivity(i);
    		
    		Toast.makeText(DMSMainActivity.this, "Prefs", Toast.LENGTH_LONG);
    		
    		break;
    	}
    	
    	return true;
    }

    public void timerClickHandler(View view) {
    	Button timerButton = (Button) findViewById(view.getId());
    	
		long endTime = SystemClock.currentThreadTimeMillis() + (1*1000);
    	
    	Log.i("Button click", "Now is: " + SystemClock.currentThreadTimeMillis());
    	Log.i("Button click", "Set to end at: " + endTime);
    	DMSCountDown cd = new DMSCountDown(endTime);
    	cd.start();
    	
    	timerButton.setText(getString(R.string.stopTimer));
    }
    
    private class DMSCountDown extends CountDownTimer {
    	private TextView timerText = (TextView) findViewById(R.id.timerText);

    	public DMSCountDown(long millisInFuture) {
			super(millisInFuture, 250);
		}

    	
		@Override
		public void onFinish() {
			timerText.setText("END!");
			
			SharedPreferences preferences =
					PreferenceManager.getDefaultSharedPreferences(DMSMainActivity.this);
	    	String phone = preferences.getString("phone", "21680621");
			long timestamp = SystemClock.currentThreadTimeMillis();
			String signature = phone+"\n"+timestamp;
			signature = "rulle";
			
			XMLRPCClient xmlRpcClient =
					new XMLRPCClient("http://192.168.2.166:8080/AlarmService/xmlrpc");
			try {
				boolean ok = ((Boolean) xmlRpcClient
						.call("AlarmService.fireAlarm"));
				Toast.makeText(DMSMainActivity.this, ""+ok, Toast.LENGTH_SHORT).show();
			} catch (XMLRPCException e) {
				Log.e("XML RPC call", "Failed", e);
			}
		}

		@Override
		public void onTick(long millisUntilFinished) {
			int seconds = (int)Math.floor(millisUntilFinished/1000);
			int minutes = seconds/60;
			seconds = seconds%60;
			
			if (seconds < 10)
				timerText.setText(minutes+":0"+seconds);
			else
				timerText.setText(minutes+":"+seconds);			
		}
    }
    
}