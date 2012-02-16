package dk.rohdef.DMS;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class DMSPreferencesActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
}
