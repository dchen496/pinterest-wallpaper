package com.example.quart;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

public class PreferencesActivity extends Activity {

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setSubtitle(R.string.title_activity_preferences);

		Bundle b = this.getIntent().getExtras();
		if (b !=null && b.getString("com.pinterest.EXTRA_PIN_ID") != null) {
			AsyncHttpClient client = new AsyncHttpClient();
			client.get("http://quart.herokuapp.com/pin_to_id?id=" + b.getString("com.pinterest.EXTRA_PIN_ID"), new AsyncHttpResponseHandler() { // TODO CHANGE GOOGLE
			    @Override
			    public void onSuccess(final String response) {
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PreferencesActivity.this);
					prefs.edit().putString("edittext_pinterest_board_url", response.trim()).commit();
			        Toast.makeText(PreferencesActivity.this, R.string.saved, Toast.LENGTH_LONG).show();
			    }
			});
		}
		setContentView(R.layout.activity_preferences);
	}
}
