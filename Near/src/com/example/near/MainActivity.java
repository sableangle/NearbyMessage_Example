package com.example.near;

import java.util.Date;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import java.text.SimpleDateFormat;
public class MainActivity extends Activity implements ConnectionCallbacks,OnConnectionFailedListener{
	
	int REQUEST_RESOLVE_ERROR = -1;
	boolean mResolvingError;
	GoogleApiClient mGoogleApiClient;
	Message mMessage;
	MessageListener mMessageListener;
	String TAG;
	
	Button Send;
	Button Get;

	private Context mContext=null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mContext = this;
		
		mGoogleApiClient = new GoogleApiClient.Builder(this)
			     .addApi(Nearby.MESSAGES_API)
			     .addConnectionCallbacks(this)
			     .addOnConnectionFailedListener(this)
			     .build();
	
		
		Date curDate = new Date(System.currentTimeMillis()) ; // 獲取當前時間
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateString = sdf.format(curDate);	
		String a = "From : " + android.os.Build.MODEL + " in " + dateString;
		mMessage = new Message(a.getBytes());
		
		Send = (Button) findViewById(R.id.btnSend);
		Send.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Date curDate = new Date(System.currentTimeMillis()) ; // 獲取當前時間
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String dateString = sdf.format(curDate);	
				String a = "From : " + android.os.Build.MODEL + " in " + dateString;
				mMessage = new Message(a.getBytes());
				publishContent(mMessage);
				showToast("Sent");
			}
			
		});
		Get = (Button) findViewById(R.id.btnGet);
		Get.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				StartlistenContent();
			}
			
		});
		mMessageListener = new MessageListener() {
			  @Override
			  public void onFound(final Message message) {
			    // Do something with message.getContent()
				  String str = new String(message.getContent());
				  showToast(str);
			  }
			};
			
	}
	@Override
	protected void onStart() {
	    super.onStart();
	    if (!mGoogleApiClient.isConnected()) {
	        mGoogleApiClient.connect();
			showToast("connect");
	    }
	}
	
	

	@Override
	protected void onStop() {
	    if (mGoogleApiClient.isConnected()) {
	        // Clean up when the user leaves the activity.
	        Nearby.Messages.unpublish(mGoogleApiClient, mMessage)
	                .setResultCallback(new ErrorCheckingCallback("unpublish()"));
	        Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener)
	                .setResultCallback(new ErrorCheckingCallback("unsubscribe()"));
	    }
	    mGoogleApiClient.disconnect();
	    super.onStop();
	}

	// GoogleApiClient connection callback.
	@Override
	public void onConnected(Bundle connectionHint) {
	    Nearby.Messages.getPermissionStatus(mGoogleApiClient).setResultCallback(
	            new ErrorCheckingCallback("getPermissionStatus", new Runnable() {
	                @Override
	                public void run() {
	    				
	                }
	            })
	    );
	}

	// This is called in response to a button tap in the Nearby permission dialog.
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    if (requestCode == REQUEST_RESOLVE_ERROR) {
	        mResolvingError = false;
	        if (resultCode == RESULT_OK) {
	            // Permission granted or error resolved successfully then we proceed
	            // with publish and subscribe..
	        } else {
	            // This may mean that user had rejected to grant nearby permission.
	            showToast("Failed to resolve error with code " + resultCode);
	        }
	    }
	}
	private void StartlistenContent(){
		// Create an instance of MessageListener		
		Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener)
        .setResultCallback(new ErrorCheckingCallback("subscribe()"));
	}
	private void publishContent(Message msg){
		Nearby.Messages.publish(mGoogleApiClient, msg)
        .setResultCallback(new ErrorCheckingCallback("publish()"));
	}

	/**
	* A simple ResultCallback that displays a toast when errors occur.
	* It also displays the Nearby opt-in dialog when necessary.
	*/
	private class ErrorCheckingCallback implements ResultCallback<Status> {
	   private final String method;
	   private final Runnable runOnSuccess;

	   private ErrorCheckingCallback(String method) {
	       this(method, null);
	   }

	   private ErrorCheckingCallback(String method, @Nullable Runnable runOnSuccess) {
	       this.method = method;
	       this.runOnSuccess = runOnSuccess;
	   }

	   @Override
	   public void onResult(@NonNull Status status) {
	       if (status.isSuccess()) {
	           Log.i(TAG, method + " succeeded.");
	           if (runOnSuccess != null) {
	               runOnSuccess.run();
	           }
	       } else {
	           // Currently, the only resolvable error is that the device is not opted
	           // in to Nearby. Starting the resolution displays an opt-in dialog.
	           if (status.hasResolution()) {
	               if (!mResolvingError) {
	                   try {
	                       status.startResolutionForResult(MainActivity.this,
	                               REQUEST_RESOLVE_ERROR);
	                       mResolvingError = true;
	                   } catch (IntentSender.SendIntentException e) {
	                       showToastAndLog(Log.ERROR, method + " failed with exception: " + e);
	                   }
	               } else {
	                   // This will be encountered on initial startup because we do
	                   // both publish and subscribe together.  So having a toast while
	                   // resolving dialog is in progress is confusing, so just log it.
	                   Log.i(TAG, method + " failed with status: " + status
	                           + " while resolving error.");
	               }
	           } else {
	               showToastAndLog(Log.ERROR, method + " failed with : " + status
	                       + " resolving error: " + mResolvingError);
	           }
	       }
	   }
	}

	private void showToastAndLog(int error, String string) {
		// TODO Auto-generated method stub
		Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
	}

	public void showToast(final String toast)
	{
		runOnUiThread(new Runnable() { @Override public void run() { Toast.makeText(mContext,toast, Toast.LENGTH_SHORT).show(); } });
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub
		showToast("onConnectionFailed");
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		// TODO Auto-generated method stub
		
	}
}