package in.bitshyderabad.csis;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.List;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import android.net.NetworkInfo;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import android.net.ConnectivityManager;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class TrackService extends Service {


	private Handler mHandler = new Handler();

	static TrackService serviceRef =null;
	String devIdentity = null;
	static TAContext ctx = null;
	static String MY_TAG="TrackSrvc";

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		serviceRef = this;
		Log.d(MY_TAG,"Service STARTED");
		ctx = TAContext.createContextFromPersistedData(this);
		mHandler.postDelayed(mRunnable, 10000);
	}

	@Override
	public void onDestroy() {
		mHandler.removeCallbacks(mRunnable);
		ctx.saveLogs();
		ctx = null; // explicit free - may not be needed
		super.onDestroy();
		Log.d(MY_TAG, "Service Destroyed");
	}

	public void displaySrvcErrorNotification(String msg, Exception e)
	{
		final int MY_NOTIFICATION_ID=2;
		NotificationManager notificationManager;
		Notification myNotification;
		final String notificationTitle = "TrackAppExcpt";
		StackTraceElement stElArr[] = e.getStackTrace();
		String text = msg + e.getMessage() +"\n";
		for(int i=0; i < stElArr.length; i++){
			text += "\t" + stElArr[i].getFileName() + ":" + stElArr[i].getMethodName() +
					":" + stElArr[i].getLineNumber() + "\n"; 
		}
		
	    Intent intent = new Intent(serviceRef, ErrorNotifyActivity.class);
	    intent.putExtra("text", text);
	    PendingIntent pIntent = PendingIntent.getActivity(serviceRef, 0, intent, 0);
	    

		Log.e(MY_TAG, text);
		notificationManager =(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		Notification.Builder notifBuild = new Notification.Builder(serviceRef.getBaseContext());
		notifBuild.setContentTitle(notificationTitle);
		//notifBuild.setContentText(text);
		notifBuild.setContentText(text);
		notifBuild.setSmallIcon(R.drawable.icon);
		notifBuild.setContentIntent(pIntent);
		//notifBuild.addAction(R.drawable.icon,"More",pIntent);
		
		myNotification = notifBuild.getNotification(); // deprecated by build in API 16 and above
		myNotification.defaults |= Notification.DEFAULT_SOUND;
		myNotification.flags |= Notification.FLAG_AUTO_CANCEL;
		notificationManager.notify(MY_NOTIFICATION_ID, myNotification);
	}

	void displaySwUpdateNotification()
	{
		final int MY_NOTIFICATION_ID=1;
		NotificationManager notificationManager;
		Notification myNotification;
		final String notificationTitle = "BITS TRACK APP NEW UPDATE";
		final String notificationText = "visit 172.16.100.162/"; // TBD URL should be an Argument based on response or move to Values file
		final String myBlog = "http://universe.bits-pilani.ac.in/hyderabad/abhishekthakur/StudentProjects";

		notificationManager =(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		Notification.Builder notifBuild = new Notification.Builder(serviceRef.getBaseContext());
		notifBuild.setContentTitle(notificationTitle);
		notifBuild.setContentText(notificationText + "refer to " + myBlog + "for more details.");
		notifBuild.setSmallIcon(R.drawable.icon);
		myNotification = notifBuild.getNotification(); // deprecated by build in API 16 and above
		myNotification.defaults |= Notification.DEFAULT_SOUND;
		myNotification.flags |= Notification.FLAG_AUTO_CANCEL;
		notificationManager.notify(MY_NOTIFICATION_ID, myNotification);

	}

	public String getCurrDate()	{
		Date cal = Calendar.getInstance().getTime();
		return DateFormat.getDateTimeInstance().format(cal); 
	}


	public String getDevIdentity(){
		return devIdentity;
	}
	

	public void getDeviceName() {
		String idIMEI=null;
		String model=null;

		TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		idIMEI =  tm.getDeviceId();
		if (idIMEI == null) idIMEI = "-srl-" + android.os.Build.SERIAL;
		model = Build.MODEL +Build.MANUFACTURER;
		devIdentity = idIMEI + model;
		devIdentity = devIdentity.trim();
	} 

	private final Runnable mRunnable = new Runnable() {
		private String version=null;
		


		public boolean isAndroidEmulator() {
			String model = Build.MODEL;
			String product = Build.PRODUCT;
			boolean isEmulator = false;
			if (product != null) {
				isEmulator = product.equals("sdk") || product.contains("_sdk") || product.contains("sdk_");
			}
			if(model != null) {
				isEmulator |= model.contains("sdk");
			}
			if(devIdentity.contains("0000000000")) isEmulator = true;
			return isEmulator;
		}

		private boolean isWifi()
		{
			NetworkInfo active_network=((ConnectivityManager)serviceRef.getSystemService(TrackService.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
			if (active_network!=null && active_network.isConnectedOrConnecting())
			{	
				if (active_network.getType()==ConnectivityManager.TYPE_WIFI)
					return true;
			}
			return isAndroidEmulator();
		}


		void initData(){
			getDeviceName();
			try {
				String pkg = serviceRef.getPackageName();
				version = serviceRef.getPackageManager().getPackageInfo(pkg, 0).versionName;
			} catch (NameNotFoundException e) {
				version = "?";
			}
		}

		//
		// This function invoked every delayTimeMilliSec
		// data init needed only on first run.
		//
		public void run() {

			if(devIdentity==null) {
				initData();
				// recover the serialized context
			}
			
			ctx.doSampling();

			if(ctx.timeToChwckAppUpdate()) 
				new CheckAppUpdate().execute(version); // will be removed once we are in App Store
			

			if(ctx.shouldCreateLogFile()){
				String fileName = ctx.saveLogs();
				if (null != fileName)
					handleLogStreamEndAndFileUpload(fileName);
			}


			mHandler.postDelayed(mRunnable, ctx.getNextPollDelay());
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

		class LogUpdateAsyncTask extends AsyncTask<String, Integer, Long>{

			protected Long doInBackground(String... strings ){
				HttpClient httpclient = new DefaultHttpClient();
				Log.d("LogUpdateAsyncTask", "coming in with argcount=" + strings.length);
				HttpPost httppost = new HttpPost("http://172.16.100.162/android/scripts/logupdate.php");

				Log.d(MY_TAG, "logupdate.php: Possible SQL command is INSERT INTO LogUploads  VALUES('','" + strings[0] +
						"','" +strings[1]+ "','" +strings[2]+ "','" +strings[3]+ "','" +strings[4]+
						"','" +strings[5]+ "','" +strings[6]+ "','" +strings[7]+ "','" +strings[8]+
						"','" +strings[9]+ "')");
				try {
					// Add your data
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(12);
					nameValuePairs.add(new BasicNameValuePair("uploadtime", strings[0]));
					nameValuePairs.add(new BasicNameValuePair("devid", strings[1]));
					nameValuePairs.add(new BasicNameValuePair("nmentries", strings[2]));
					nameValuePairs.add(new BasicNameValuePair("filename", strings[3]));
					nameValuePairs.add(new BasicNameValuePair("swversion", strings[4]));
					nameValuePairs.add(new BasicNameValuePair("nwoperator", strings[5]));
					nameValuePairs.add(new BasicNameValuePair("starttime", strings[6]));
					nameValuePairs.add(new BasicNameValuePair("endtime", strings[7]));
					nameValuePairs.add(new BasicNameValuePair("longitude", strings[8]));
					nameValuePairs.add(new BasicNameValuePair("latitude", strings[9]));

					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

					// Execute HTTP Post Request
					HttpResponse response = httpclient.execute(httppost);
					String res=EntityUtils.toString(response.getEntity());
					Log.d(MY_TAG, "serverresponse:logupdate.php"  + res + "\nStatus:" + response.getStatusLine());
				} catch (Exception e) {
					displaySrvcErrorNotification("Update DB on upload",e);
				}
				return null;
			}
		}

		void postdata(String s1,String s2,String s3,String s4,String s5,String s6,String s7,String s8,String s9,String s10) {
			Log.d("postdata", "going in");
			new LogUpdateAsyncTask().execute(s1,s2,s3,s4,s5,s6,s7,s8,s9,s10);
			Log.d("postdata", "going out");
		}


		void zip(String fileName) {
			final int BUFFER_SIZE = 1024;
			BufferedInputStream origin = null;
			ZipOutputStream out = null;
			
			try {
				out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(fileName+".zip")));
				byte data[] = new byte[BUFFER_SIZE];

				FileInputStream fi = new FileInputStream(fileName);    
				origin = new BufferedInputStream(fi, BUFFER_SIZE);
				try {
					ZipEntry entry = new ZipEntry(fileName.substring(fileName.lastIndexOf("/") + 1));
					out.putNextEntry(entry);
					int count;
					while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
						out.write(data, 0, count);
					}
				} catch(Exception e){
					displaySrvcErrorNotification("FizeZIP Entry:"+fileName,e);
				} finally {
					origin.close();
				}
			}catch(Exception e){
				displaySrvcErrorNotification("FizeZIP Out:"+fileName,e);
			}finally {
				try {
					if(null != out) out.close();
				} catch(Exception e){
					displaySrvcErrorNotification("FizeZIP Close:"+fileName,e);
				}
			}
			File file = new File(fileName);
			Log.d("ZIP", "removing unzipped file " + fileName + "of size" + file.length());
			file.delete();
		}

		void handleLogStreamEndAndFileUpload(String currentFilePath)
		{

			String str = LoadPreferences("fileuploads");
			int lastuploadwifi =Integer.parseInt(str);
			str = LoadPreferences("createdFileIdx");
			int currUploadIndex  = Integer.parseInt(str);

			if(isWifi()) 
			{
				Log.d(MY_TAG, "detected wifi/emulator");
				lastuploadwifi=Integer.parseInt(LoadPreferences("fileuploads").toString());
				for(int i=lastuploadwifi;i<currUploadIndex;i++)
				{
					// compress the file currentFilePath
					String xmlFileName = devIdentity+i +".xml";
					String localFileName = "/data/data/in.bitshyderabad.csis/files/"+xmlFileName;
					File file=new File(localFileName);
					if(file.length()>100) { // XML exists - zip it 
						zip(localFileName);
						Log.d(MY_TAG, "file zipped to:"+currentFilePath+".zip");
					}
					
					// upload the zipped file
					String fileName = devIdentity+i +".xml.zip";
					localFileName = "/data/data/in.bitshyderabad.csis/files/"+fileName;
					String date = getCurrDate();
					file=new File(localFileName);
					int size=(int) (file.length()/1024) +1;
					if(file.length() > 100) { // discard non-existent / small size files
						new DoFileUpload().execute(localFileName);
						postdata(date,devIdentity,	Integer.toString(i),fileName,
								version,ctx.get_network(),date/*should be log start time */,
								date,ctx.getCurrentLocation(),ctx.getCurrentLocation()); 
						// TBD remove second location in PHP and here
						
						int tempsize=Integer.parseInt(LoadPreferences("bandwidth"));
						tempsize+=size;
						SavePreferences("bandwidth",String.valueOf(tempsize));
						if( file.delete())
						{
							Log.d(MY_TAG, "file delete succesfull" + file.getName());
						}
					}
					SavePreferences("fileuploads",Integer.toString(i) );
				}
			}
			else
			{ 
				Toast.makeText(getApplicationContext(), "wifinotdetected", Toast.LENGTH_SHORT).show();
				Log.w(MY_TAG, "NO WiFi detected");
			}	
		}



		class DoFileUpload extends AsyncTask<String, Integer, Long>{

			protected Long doInBackground(String... strings ){
				HttpURLConnection connection = null;
				DataOutputStream outputStream = null;
				@SuppressWarnings("unused")
				DataInputStream inputStream = null;

				String pathToOurFile = strings[0];
				Log.d("upload",strings[0]);
				String urlServer = "http://172.16.100.162/android/scripts/uploadfile.php";
				String lineEnd = "\r\n";
				String twoHyphens = "--";
				String boundary =  "*****";

				int bytesRead, bytesAvailable, bufferSize;
				byte[] buffer;
				int maxBufferSize = 1*1024*1024;

				try
				{
					FileInputStream fileInputStream = new FileInputStream(new File(pathToOurFile) );

					URL url = new URL(urlServer);
					connection = (HttpURLConnection) url.openConnection();

					// Allow Inputs & Outputs
					connection.setDoInput(true);
					connection.setDoOutput(true);
					connection.setUseCaches(false);

					// Enable POST method
					connection.setRequestMethod("POST");

					connection.setRequestProperty("Connection", "Keep-Alive");
					connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);

					outputStream = new DataOutputStream( connection.getOutputStream() );
					outputStream.writeBytes(twoHyphens + boundary + lineEnd);
					outputStream.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + pathToOurFile +"\"" + lineEnd);
					outputStream.writeBytes(lineEnd);

					bytesAvailable = fileInputStream.available();
					bufferSize = Math.min(bytesAvailable, maxBufferSize);
					buffer = new byte[bufferSize];

					// Read file
					bytesRead = fileInputStream.read(buffer, 0, bufferSize);

					while (bytesRead > 0)
					{
						outputStream.write(buffer, 0, bufferSize);
						bytesAvailable = fileInputStream.available();
						bufferSize = Math.min(bytesAvailable, maxBufferSize);
						bytesRead = fileInputStream.read(buffer, 0, bufferSize);
					}

					outputStream.writeBytes(lineEnd);
					outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

					// Responses from the server (code and message)
					int respCode = connection.getResponseCode();
					String respMsg = connection.getResponseMessage();
					Log.d(MY_TAG, pathToOurFile + "success in  uploadfile.php "  + respCode + "\nMessage:" +respMsg);

					fileInputStream.close();
					outputStream.flush();
					outputStream.close();
				}
				catch (Exception ex)
				{
					displaySrvcErrorNotification(pathToOurFile +"uploadfile.php",ex);
				}
				return null;
			}
		}



		class CheckAppUpdate extends AsyncTask<String, Integer, Long>{
			boolean notify =false;

			protected Long doInBackground(String... strings ){
				HttpClient httpclient = new DefaultHttpClient();
				HttpPost httppost = new HttpPost("http://172.16.100.162/android/scripts/updateversion.php");

				try {
					// Add your data
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
					nameValuePairs.add(new BasicNameValuePair("currentversion", strings[0]));
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

					// Execute HTTP Post Request
					HttpResponse response = httpclient.execute(httppost);
					String res=EntityUtils.toString(response.getEntity());
					Log.d("serverresponse",res);
					if(res.equals("update"))
					{
						notify = true;
					}
				} catch (Exception e) {
					displaySrvcErrorNotification("Check for Version Update",e);
				} 
				return null;
			}

			protected void onPostExecute(Long result){
				if(notify)displaySwUpdateNotification();
			}
		}

	};
}
