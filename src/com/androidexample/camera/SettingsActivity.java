/**
 * 
 */
package com.androidexample.camera;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

/**
 * @author emersont
 *
 */
public class SettingsActivity extends Activity {
	
	private static final String TAG = "SettingsActivity";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "onCreate*************************************");
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    
    @Override
    protected void onStart() {
    	Log.d(TAG, "onStart*************************************");

    }
    
    @Override
    protected void onPause() {
    	Log.d(TAG, "onPause*************************************");

    }

    @Override
    protected void onRestart() {
    	Log.d(TAG, "onRestart*************************************");

    }
    
    @Override
    protected void onResume() {
    	Log.d(TAG, "onResume*************************************");

    }

    @Override
    protected void onStop() {
    	Log.d(TAG, "onStop*************************************");

    }
    
    @Override
    protected void onDestroy() {
    	Log.d(TAG, "onDestroy*************************************");

    }
    
}