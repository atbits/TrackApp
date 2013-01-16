package in.bitshyderabad.csis;


import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;



import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;






public class MainActivity extends Activity implements View.OnClickListener{

	/** Called when the activity is first created. */
	String devIdentity=null;
	boolean registered =false;
	String username1=null;
	EditText  editIdName, editEMail, editUri, editPosalAddress;
	Button button1,button2;
	ToggleButton toggleButton1;
	IntentFilter intentFilter;
	HttpPost httppost=null;
	TextView text;

	String getAppBuildTime(){
		String timeStr = "unknown";
		try{
		     ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), 0);
		     ZipFile zf = new ZipFile(ai.sourceDir);
		     ZipEntry ze = zf.getEntry("classes.dex");
		     long time = ze.getTime();
		     timeStr = SimpleDateFormat.getInstance().format(new java.util.Date(time));

		  }catch(Exception e){
		  }
		return timeStr;
	}
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		getDeviceIdentity();

		if(!(LoadPreferences("register").equalsIgnoreCase("true")))
		{
			setContentView(R.layout.main);
			initialize();
			button2.setEnabled(false);
			button2.setVisibility(View.INVISIBLE);
		}
		else
		{
			setContentView(R.layout.register);
			text=(TextView) findViewById(R.id.bandwidth);
			String textStr = "TracApp uploaded "+LoadPreferences("bandwidth")+" Kilobytes\n" + getAppBuildTime();
			text.setText(textStr);
			button2=(Button) findViewById(R.id.register1);
			button2.setOnClickListener(new View.OnClickListener() 
			{
				public void onClick(View v) {
					setContentView(R.layout.main);
					initialize();
					editIdName.setText(LoadPreferences("name"));
					editEMail.setText(LoadPreferences("email"));
					editPosalAddress.setText(LoadPreferences("address"));
					editUri.setText(LoadPreferences("uri"));
					editEMail.setKeyListener(null);
					editUri.setKeyListener(null);
					button1.setEnabled(false);
					button1.setVisibility(View.INVISIBLE);
				}
			});
			Intent serviceIntent = new Intent();
			serviceIntent.setAction("in.bitshyderabad.csis.TrackService");
			startService(serviceIntent);
		}

		if(!isNetworkAvailable()) {
			Toast.makeText(getApplicationContext(), "please connect to internet and try again", Toast.LENGTH_LONG).show();
		} else	{
			Log.d("first","machine is connected to internet");
		}
		// we take the liberty to start the service even if user has not registered yet
	}



	private void initialize() {
		editIdName = (EditText) findViewById(R.id.editIdName);
		editEMail = (EditText) findViewById(R.id.editEMail);
		editUri = (EditText) findViewById(R.id.editUri);
		editPosalAddress = (EditText) findViewById(R.id.editPosalAddress);
		button1 = (Button) findViewById(R.id.button1);
		button2= (Button) findViewById(R.id.button2);
		toggleButton1 = (ToggleButton) findViewById(R.id.toggleButton1);
		button1.setOnClickListener(this);
		button2.setOnClickListener(this);
	}

	public String getDeviceIdentity() {
		String idIMEI=null;
		String model=null;

		TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		idIMEI =  tm.getDeviceId();
		if (idIMEI == null) idIMEI = "-srl-" + android.os.Build.SERIAL;
		model = Build.MODEL +Build.MANUFACTURER;
		devIdentity = idIMEI + model;
		return devIdentity;
	} 


	public void onClick(View v) {

		switch(v.getId()){

		case R.id.button1:
			SavePreferences("bandwidth","0");
			String name=editIdName.getEditableText().toString();
			String email=editEMail.getEditableText().toString();
			String uri=editUri.getEditableText().toString();
			String address=editPosalAddress.getEditableText().toString();
			if( name.length()==0||uri.length()==0||email.length()==0)
			{
				Toast.makeText(getApplicationContext(), "please fill the mandatory fields and try again", Toast.LENGTH_LONG).show();
			}
			//
			else{
				SavePreferences("email",email);
				SavePreferences("uri",uri);
				SavePreferences("address",address);
				editEMail.setKeyListener(null);
				editUri.setKeyListener(null);

				SavePreferences("register","true");
				username1=editIdName.getText().toString();
				SavePreferences("name", username1);
				Log.d("onClick", "Register requested for " + LoadPreferences("name"));

				if(toggleButton1.isChecked())
					SavePreferences("mask", "on");
				else
					SavePreferences("mask", "off");
				Log.d("masking option is ",LoadPreferences("mask"));
				Log.d("onClick", "Register requested for " + LoadPreferences("name"));
				Log.d("masking option is ",LoadPreferences("mask"));

				Intent serviceIntent = new Intent();
				serviceIntent.setAction("in.bitshyderabad.csis.TrackService");
				startService(serviceIntent);

				
				if(isNetworkAvailable())
				{
					Log.d("posted","machine is connected to internet");
					new RegisterDeviceTask().execute(name, 
							email, 
							uri, 
							address,devIdentity,"register");

					Context context = getApplicationContext();
					String reg = LoadPreferences("name")+" has been registered on the network";
					int duration = Toast.LENGTH_SHORT;

					Toast toast = Toast.makeText(context, reg, duration);
					toast.show();
					finish();
				} 
				else
				{
					Toast.makeText(getApplicationContext(), "please connect to internet and try again", Toast.LENGTH_LONG).show();
					break;
				}
			}


			break;
		case R.id.button2:
		{
			String altname=editIdName.getEditableText().toString();
			String altemail=editEMail.getEditableText().toString();
			String alturi=editUri.getEditableText().toString();
			String altaddress=editPosalAddress.getEditableText().toString();
			if(alturi.length()==0||altemail.length()==0)
			{
				Toast.makeText(getApplicationContext(), "please fill the mandatory fields and try again", Toast.LENGTH_LONG).show();
			}
			//
			else{
				SavePreferences("email",altemail);
				SavePreferences("uri",alturi);
				SavePreferences("address",altaddress);
				editEMail.setKeyListener(null);
				editUri.setKeyListener(null);

				SavePreferences("register","true");
				username1=editIdName.getText().toString();
				SavePreferences("name", username1);
				Log.d("onClick", "Register requested for " + LoadPreferences("name"));

				if(toggleButton1.isChecked())
					SavePreferences("mask", "on");
				else
					SavePreferences("mask", "off");
				Log.d("masking option is ",LoadPreferences("mask"));
				Log.d("onClick", "Register requested for " + LoadPreferences("name"));
				Log.d("masking option is ",LoadPreferences("mask"));
				if(isNetworkAvailable())
				{
					Log.d("posted","machine is connected to internet");
					new RegisterDeviceTask().execute(altname, 
							altemail, 
							alturi, 
							altaddress,devIdentity,"update");

					Context context = getApplicationContext();
					String reg = LoadPreferences("name")+"has been registered on the network";
					int duration = Toast.LENGTH_SHORT;

					Toast toast = Toast.makeText(context, reg, duration);
					toast.show();
					finish();
				} 
				else
				{
					Toast.makeText(getApplicationContext(), "please connect to internet and try again", Toast.LENGTH_LONG).show();
					break;
				}
			}

		}
		default:

		}


	}
	private class RegisterDeviceTask extends AsyncTask<String, Integer, Long>{

		protected Long doInBackground(String... strings ){

			try{
				RegisterUser(strings[0],strings[1],strings[2],strings[3],strings[4],strings[5]);
				Log.d("task","taskover");
			}
			catch (Exception e) {
				Toast.makeText(getApplicationContext(), "CPPe" + e.getMessage(), Toast.LENGTH_LONG).show();
				Log.e("PostActivity: ClientProtoException", e.getMessage());
				Log.e("PostActivity: ClientProtoException", e.getStackTrace().toString());
			}
			return null;

			// Create a new HttpClient and Post Header

		}

		protected void onProgressUpdate(Integer... progress){ /* does nothing */}
		protected void onPostExecute(Long result){/* does nothing */}
	}
	
	public void RegisterUser(String s1,String s2,String s3,String s4,String s5,String s6) {
		// Create a new HttpClient and Post Header
		HttpClient httpclient = new DefaultHttpClient();
		if(s6.equals("register"))
			httppost = new HttpPost("http://172.16.100.162/android/scripts/user.php");
		else
			httppost = new HttpPost("http://172.16.100.162/android/scripts/updateuser.php");

		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);
			nameValuePairs.add(new BasicNameValuePair("name", s1));
			nameValuePairs.add(new BasicNameValuePair("email", s2));
			nameValuePairs.add(new BasicNameValuePair("location", s3));
			nameValuePairs.add(new BasicNameValuePair("contact", s4));
			nameValuePairs.add(new BasicNameValuePair("DeviceId", s5));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			Log.d("result","Trying to post");
			HttpResponse response = httpclient.execute(httppost);
			String res=EntityUtils.toString(response.getEntity());
			Log.d("serverresponse",res);
			Log.d("result","POSTED");
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), "CPPe" + e.getMessage(), Toast.LENGTH_LONG).show();
			Log.e("PostActivity: ClientProtoException", e.getMessage());
			Log.e("PostActivity: ClientProtoException", e.getStackTrace().toString());
		} 
	} 

	private void SavePreferences(String key, String value){
		SharedPreferences sharedPreferences = getSharedPreferences("MY_SHARED_PREF", MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString(key, value);
		editor.commit();
	}
	private String LoadPreferences(String s){
		SharedPreferences sharedPreferences =  getSharedPreferences("MY_SHARED_PREF", MODE_PRIVATE);;
		String name = sharedPreferences.getString(s, "");
		return name;
	}
	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager 
		= (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null;
	}
}


