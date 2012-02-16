package dk.rohdef.DMS;

import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
    	
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    	String username = preferences.getString("phone", "");
    	// Date/time
    	long timestamp = new Date().getTime();
    	String signature = username+"\n"+timestamp;
    	Toast.makeText(DMSMainActivity.this, signature, Toast.LENGTH_LONG).show();
    	
    	timerButton.setText(getString(R.string.stopTimer));
    }
}

