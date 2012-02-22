package dk.rohdef.DMS;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
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

	private DMSCountDown countDown;
	private DMSAlarmCountDown alarmCountDown;
	private boolean running;

    /**
     * Called when the activity is first created.
     * @param savedInstanceState If the activity is being re-initialized after 
     * previously being shut down then this Bundle contains the data it most 
     * recently supplied in onSaveInstanceState(Bundle). <b>Note: Otherwise it is null.</b>
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        running = false;
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
    
    	// TODO should I lock this
    	if (running) {
    		alarmCountDown.cancel();
    		countDown.cancel();
    		timerButton.setText(R.string.startTimer);
    		running = false;
    	} else {
    		running = true;
    		
    		long endTime = SystemClock.currentThreadTimeMillis() + (15*60*1000);
    		long alarmTime = SystemClock.currentThreadTimeMillis() + (10*60*1000);
    		
    		countDown = new DMSCountDown(endTime);
    		countDown.start();
    		alarmCountDown = new DMSAlarmCountDown(alarmTime);
			alarmCountDown.start();

    		timerButton.setText(getString(R.string.stopTimer));
    	}
    }
    
    private class DMSAlarmCountDown extends CountDownTimer {
    	private TextView timerText = (TextView) findViewById(R.id.timerText);
    	public DMSAlarmCountDown(long miliesInFuture) {
    		super(miliesInFuture, 500);
    	}
    	
    	@Override
    	public void onFinish() {
    		timerText.setTextColor(Color.RED);
    		
    		Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
    		if (alert == null)
    			alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    		if (alert == null)
    			alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
    		
    		MediaPlayer mediaPlayer = new MediaPlayer();
    		try {
				mediaPlayer.setDataSource(DMSMainActivity.this, alert);
			} catch (IllegalArgumentException e) {
				Log.e("Preparing alarm", "", e);
			} catch (SecurityException e) {
				Log.e("Preparing alarm", "", e);
			} catch (IllegalStateException e) {
				Log.e("Preparing alarm", "", e);
			} catch (IOException e) {
				Log.e("Preparing alarm", "", e);
			}
    		
    		AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
    		audioManager.setStreamVolume(AudioManager.STREAM_ALARM,
    				audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
    				AudioManager.FLAG_PLAY_SOUND);
    		
    		mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
    		mediaPlayer.setLooping(false);
    		try {
				mediaPlayer.prepare();
			} catch (IllegalStateException e) {
				Log.e("Preparing to play alarm", "", e);
			} catch (IOException e) {
				Log.e("Preparing to play alarm", "", e);
			}
    		mediaPlayer.start();
    	}
    	
    	@Override
    	public void onTick(long millisUntilFinished) {
    		// DO nothing, not needed
    	}
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
			// TODO argh, int please! (Or something similar)
	    	String phone = preferences.getString("phone", "21680621");
			String uuid = UUID.randomUUID().toString();
			
			byte[] signature = createSignature(phone, uuid);
			
			XMLRPCClient xmlRpcClient =
					new XMLRPCClient("http://asterisk2go:8080/AlarmService/xmlrpc");
			try {
				boolean ok = ((Boolean) xmlRpcClient
						.callEx("AlarmService.fireAlarm",
								Integer.parseInt(phone),
								uuid,
								signature));
				Toast.makeText(DMSMainActivity.this, ""+ok, Toast.LENGTH_SHORT).show();
			} catch (XMLRPCException e) {
				Log.e("XML RPC call", "Failed", e);
			}
		}

		private byte[] createSignature(String phone, String uuid) {
			String rawSignature = phone + uuid;

			PrivateKey key = readPrivateKeyFromFile();
			byte[] cipherData = null;
			try {
				Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				cipher.init(Cipher.ENCRYPT_MODE, key);
				cipherData = cipher.doFinal(rawSignature.getBytes("UTF8"));
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				e.printStackTrace();
			} catch (BadPaddingException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			return cipherData;
		}

		private PrivateKey readPrivateKeyFromFile() {
			PrivateKey key = null;
			try {
				InputStream fileStream = getResources()
						.openRawResource(R.raw.mykey);
				DataInputStream dataInput = new DataInputStream(
						fileStream);

				byte[] buffer = new byte[1024];
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				while (true) {
					int n = dataInput.read(buffer);
					if (n<0) break;
					baos.write(buffer, 0, n);
				}
				
				dataInput.close();
				
				PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(baos.toByteArray());
				KeyFactory keyFactory = KeyFactory.getInstance("RSA");
				key = keyFactory.generatePrivate(spec);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (InvalidKeySpecException e) {
				Log.e("Open private key", "Invalid key specification", e);
			}
			return key;
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