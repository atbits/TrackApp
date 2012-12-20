package in.bitshyderabad.csis;





import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;
import android.os.Bundle;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.NetworkInfo;
import android.net.ConnectivityManager;import android.net.TrafficStats;
import android.net.Uri;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlSerializer;
public class MyService extends Service {

	// moving average done over this
	// threshold in KBPS where we say streaming is possibly ongoing;
	// if streamed for next 5 FAST_SAMPLES we treat it as streaming
	public String FilePath=null;
	String manufacturer=null;
	String model=null;
	static int userfocus=10;
	public String starttime=null;
	static int speedentry=1;
	static int numberentry=1;
	public boolean isstreaming=false;
	private Handler mHandler = new Handler();
	
	// stream filter: Min 60 seconds duration for streaming to be caught
	// we do not want to check more agresively than 30 seconds
	// in 10 samples (of 3 seconds each) at least 6 show some data transfer
	
	final private int numSamples = 10; 
	final private long RX_TSH = 8; 
	final private int FAST_SAMPLE = 3000; // Three seconds
	final private int SLOW_SAMPLE = 30000; // thirty seconds
	final private int NUMBER_NODES=500;

	private static final long MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 10; // in Meters
	private static final long MINIMUM_TIME_BETWEEN_UPDATES = 1000; // in Milliseconds

	final static String ACTION = "in.bitshyderabad.csis.MyService";
	
	FileOutputStream fileos=null;
	static int servicecount=1;
	static int processcount=1;
	protected LocationManager locationManager;
	private final String myBlog = "http://universe.bits-pilani.ac.in/hyderabad/abhishekthakur/StudentProjects";
	private long avgspeed =0;
	private long num=1;
	private long mStartRX = 0;
	private long mStartTX = 0;
	public long mAvgRX = 0;
	public long mAvgTX = 0;
	private long sampleTX[] = new long[numSamples]; 
	private long streamDetectCount=0;
	private int sampleNum =0;
	private String longitude="location masked";
	private String latitude="location masked";
	XmlSerializer serializer = null;
	ActivityManager actvityManager=null;
	List<ActivityManager.RunningServiceInfo> l=null;
	int delayTimeMilliSec=SLOW_SAMPLE;
	static String MY_TAG="MyService";
	List<RunningAppProcessInfo> procInfos=null;
	static HashMap<String, String> processmap=null;
	List<RunningTaskInfo> taskInfo=null;
	public int updatecount=1;
	private static final int MY_NOTIFICATION_ID=1;
	private NotificationManager notificationManager;
	private Notification myNotification;
	private String version="1";
	private boolean fileupload=true;
	private int lastuploadwifi=1;
	private int lastuploadnowifi=1;
	@Override
	public IBinder onBind(Intent intent) {

		return null;

	}

	@Override

	public void onCreate() {


		SavePreferences("fileuploads","1");
		getDeviceName();
		//---

		Log.d(MY_TAG,"APPLICATION STARTED");
		actvityManager= (ActivityManager)this.getSystemService( ACTIVITY_SERVICE );
		//super.onCreate(savedInstanceState);
		//setContentView(R.layout.main);
		mStartRX = TrafficStats.getTotalRxBytes();
		mStartTX = TrafficStats.getTotalTxBytes();
		if (mStartRX == TrafficStats.UNSUPPORTED || mStartTX == TrafficStats.UNSUPPORTED) {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle("Uh Oh!");
			alert.setMessage("Your device does not support traffic stat monitoring.");
			alert.show();
		} else {
			processmap = new HashMap<String, String>();
			mHandler.postDelayed(mRunnable, 10000);
		}


	}



	private final Runnable mRunnable = new Runnable() {

		public void run() {

			updatecount++;
			if(updatecount==100)
			{
				appupdate(version);

				updatecount=1;
			}

			FilePath=("/data/data/in.bitshyderabad.csis/files/").trim().concat(LoadPreferences("name"))+manufacturer+model.concat(LoadPreferences("fileuploads").toString()).concat(".txt");


			procInfos = actvityManager.getRunningAppProcesses();
			String packageName = actvityManager.getRunningTasks(1).get(0).topActivity.getPackageName();


			l=actvityManager.getRunningServices(50);
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);



			Log.d("number of nodes",Integer.toString(numberentry));
			// Receive path logic
			long numBytes = TrafficStats.getTotalRxBytes();
			long rxBytesKBps= ((numBytes-mStartRX )/(delayTimeMilliSec));
			mStartRX = numBytes;


			// we discard the fact that some samples are smaller time than other - approximation
			mAvgRX = rxBytesKBps ;//+ numSamples *  mAvgRX - sampleRX[sampleNum % numSamples]) / numSamples;
			if(isstreaming)
			{

				try {
					checknewprocess();

					if(userfocus==10)
					{
						serializer.startTag(null,"process");
						serializer.attribute(serializer.getNamespace(), "ID",Integer.toString(processcount));
						serializer.attribute(serializer.getNamespace(), "userfocus","yes" );
						serializer.attribute(serializer.getNamespace(), "TIME STAMP",getCurrDate());
						serializer.text(packageName);
						serializer.endTag(null,"process");	
						processcount++;
						--userfocus;
					}
					else
						--userfocus;
					if(userfocus==0)
						userfocus=10;
					++numberentry;
					serializer.startTag(null,"speed");
					serializer.attribute(serializer.getNamespace(), "ID",Integer.toString(speedentry));
					serializer.attribute(serializer.getNamespace(), "NWOperator",get_network());
					serializer.attribute(serializer.getNamespace(), "TIME STAMP",getCurrDate());
					serializer.text(Long.toString(mAvgRX));
					serializer.endTag(null,"speed");
					++speedentry;

				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				avgspeed=(avgspeed+mAvgRX)/num;	
				num++;
			}

			// Transmit path logic
			numBytes = TrafficStats.getTotalTxBytes();
			long txBytesKBps= ((numBytes-mStartTX )/(delayTimeMilliSec));
			mStartTX = numBytes;


			// we discard the fact that some samples are smaller time than other - approximation
			mAvgTX = (txBytesKBps + numSamples *  mAvgTX - sampleTX[sampleNum % numSamples]) / numSamples;


			// if mAvgRX > RX_TSH ; delayTimeMilliSec is FAST_SAMPLE; if mAvgRX < RX_TSH/2 delayTimeMilliSec is SLOW_SAMPLE
			if((mAvgRX > RX_TSH)) {  // Note TX threshold is higher priority to detect streaming
				if (delayTimeMilliSec == SLOW_SAMPLE) 
					delayTimeMilliSec =FAST_SAMPLE;
				streamDetectCount++;
			}
			else if( mAvgRX < (RX_TSH/2)){ // need to adjust both TX & RX
				if (delayTimeMilliSec == FAST_SAMPLE) delayTimeMilliSec = SLOW_SAMPLE;
				if(streamDetectCount > 0) {
					try {

						
						
						if(numberentry>NUMBER_NODES)
						{	
							streamend();
							Toast.makeText(getApplicationContext(), "afterstreamed", Toast.LENGTH_LONG).show();
							Log.d("reached","afterstreamend");
						if(wifi().equals("WIFI"))
						{
							Log.d("detected","wifi");
							lastuploadwifi=Integer.parseInt(LoadPreferences("fileuploads").toString());
							if(lastuploadwifi!=lastuploadnowifi)
							{
								Toast.makeText(getApplicationContext(), "wifidetectedloop", Toast.LENGTH_LONG).show();
								for(int i=lastuploadnowifi;i<lastuploadwifi;i++)
								{
									
								fileupload(("/data/data/in.bitshyderabad.csis/files/").trim().concat(LoadPreferences("name"))+manufacturer+model.concat(Integer.toString(i)).concat(".txt"));
								postdata(getCurrDate(),LoadPreferences("name")+manufacturer+model,Integer.toString(i),LoadPreferences("name")+manufacturer+model+Integer.toString(i),version,get_network(),starttime,getCurrDate(),longitude,latitude);
								File file=new File(("/data/data/in.bitshyderabad.csis/files/").trim().concat(LoadPreferences("name"))+manufacturer+model.concat(Integer.toString(i)).concat(".txt"));
								int size=(int) (file.length()/1024);
								int tempsize=Integer.parseInt(LoadPreferences("bandwidth"));
								tempsize+=size;
								SavePreferences("bandwidth",String.valueOf(tempsize));
								if( file.delete())
								{
										Log.d("file delete","succesfull");
								}
								}
								lastuploadnowifi=lastuploadwifi;
								fileupload=true;
								
							}
							
									fileupload(FilePath);
									postdata(getCurrDate(),LoadPreferences("name")+manufacturer+model,LoadPreferences("Fileuploads"),LoadPreferences("name")+manufacturer+model+LoadPreferences("fileuploads"),version,get_network(),starttime,getCurrDate(),longitude,latitude);
									File file=new File(FilePath);
									int size=(int) (file.length()/1024);
									int tempsize=Integer.parseInt(LoadPreferences("bandwidth"));
									tempsize+=size;
									SavePreferences("bandwidth",String.valueOf(tempsize));
									if( file.delete())
									{
											Log.d("file delete","succesfull");
									}
									lastuploadwifi=lastuploadnowifi;
									fileupload=true;
									String s=LoadPreferences("fileuploads");
									int temp=Integer.parseInt(s);
									++temp;
									lastuploadnowifi=temp;
									SavePreferences("fileuploads",Integer.toString(temp) );
									Toast.makeText(getApplicationContext(), "wifidetectednmnmnm", Toast.LENGTH_LONG).show();
								
						}
						else
							
						{ 
							Toast.makeText(getApplicationContext(), "wifinotdetected", Toast.LENGTH_LONG).show();
							Log.d("detected","nowifi");
							if(fileupload)
							{
							fileupload=false;
							lastuploadnowifi=Integer.parseInt(LoadPreferences("fileuploads"));
							}
							String s=LoadPreferences("fileuploads");
							int temp=Integer.parseInt(s);
							++temp;
							SavePreferences("fileuploads",Integer.toString(temp) );
							}	
						numberentry=1;
						speedentry=1;
						servicecount=1;
						processcount=1;
						}
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalStateException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Log.d(MY_TAG,"Detected end of Streaming");
					if(LoadPreferences("mask")=="off")
					{
						locationManager.requestLocationUpdates(
								LocationManager.GPS_PROVIDER, 
								MINIMUM_TIME_BETWEEN_UPDATES, 
								MINIMUM_DISTANCE_CHANGE_FOR_UPDATES,
								new MyLocationListener());
						showCurrentLocation();
					}



					Log.d("avgspeed",Long.toString(avgspeed));
					avgspeed=0;
					num=1;
					Toast.makeText(getApplicationContext()," VIDEO STREAM DETECTED",Toast.LENGTH_LONG ).show();
					isstreaming=false;
				}
				streamDetectCount =0;
				isstreaming=false;
				avgspeed=0;
				num=1;
			}

			if((streamDetectCount>0)){ // Some streaming is happening on downside
				if(numberentry==1)
				{	starttime=getCurrDate();
				try{
					fileos =
						openFileOutput((LoadPreferences("name")).trim()+manufacturer+model.concat(LoadPreferences("fileuploads")).concat(".txt"),
								Context.MODE_APPEND);

				}catch(IOException e){
					Log.e("IOException", "exception in createNewFile() method");
				}
				//we have to bind the new file with a FileOutputStream

				//we create a XmlSerializer in order to write xml data



				serializer=Xml.newSerializer();
				try {
					serializer.setOutput(fileos, "UTF-8");
					serializer.startDocument(null, Boolean.valueOf(true)); 
					//set indentation option
					serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true); 
					//start a tag called "root"
					serializer.startTag(null, "Logfile"); 
					++numberentry;
					streamstart();

				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				//Write <?xml declaration with encoding (if encoding not null) and standalone flag (if standalone not null) 

				}
				if(streamDetectCount==1){
					try {

					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalStateException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}}
				isstreaming=true;


			}


			mHandler.postDelayed(mRunnable, delayTimeMilliSec);
		}
	};



	public String get_network()
	{

		String network_type="UNKNOWN";//maybe usb reverse tethering
		NetworkInfo active_network=((ConnectivityManager)this.getSystemService(MyService.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
		if (active_network!=null && active_network.isConnectedOrConnecting())
		{
			if (active_network.getType()==ConnectivityManager.TYPE_WIFI)
			{
				network_type="WIFI";
			}
			else if (active_network.getType()==ConnectivityManager.TYPE_MOBILE)
			{
				network_type=((ConnectivityManager)this.getSystemService(MyService.CONNECTIVITY_SERVICE)).getActiveNetworkInfo().getSubtypeName();
			}
		}
		return network_type;
	}


	public Boolean process()
	{
		for(int i = 0; i < procInfos.size(); i++)
		{
			if(procInfos.get(i).processName.equals("com.android.browser")) {
				return true;

			}

		}
		return false;
	}


	public Boolean service()
	{
		ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> l = am.getRunningServices(50);
		Iterator<ActivityManager.RunningServiceInfo> i = l.iterator();
		while (i.hasNext()) {
			ActivityManager.RunningServiceInfo runningServiceInfo = (ActivityManager.RunningServiceInfo) i.next();

			if( runningServiceInfo.service.getClassName().equals("com.android.providers.downloads.DownloadService") )
			{
				//Toast.makeText(getApplicationContext(), "download is happening", Toast.LENGTH_LONG).show();
				return true;
			}
			return false;
		}
		return false;
	}




	@Override
	public void onDestroy() {
		super.onDestroy();

		Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
		Log.d(MY_TAG, "Service Destroyed");

	}

	@Override
	public void onStart(Intent intent, int startId) {

		super.onStart(intent, startId);

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
	public void postdata(String s1,String s2,String s3,String s4,String s5,String s6,String s7,String s8,String s9,String s10) {
		// Create a new HttpClient and Post Header
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost("http://172.16.100.162/test/upload/up.php");

		try {
			// Add your data
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(12);
			nameValuePairs.add(new BasicNameValuePair("uploadtime", s1));
			nameValuePairs.add(new BasicNameValuePair("devid", s2));
			nameValuePairs.add(new BasicNameValuePair("nmentries", s3));
			nameValuePairs.add(new BasicNameValuePair("filename", s4));
			nameValuePairs.add(new BasicNameValuePair("swversion", s5));
			nameValuePairs.add(new BasicNameValuePair("nwoperator", s6));
			nameValuePairs.add(new BasicNameValuePair("starttime", s7));
			nameValuePairs.add(new BasicNameValuePair("endtime", s8));
			nameValuePairs.add(new BasicNameValuePair("longitude", s9));
			nameValuePairs.add(new BasicNameValuePair("latitude", s10));

			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			// Execute HTTP Post Request
			HttpResponse response = httpclient.execute(httppost);
			String res=EntityUtils.toString(response.getEntity());
			Log.d("serverresponse",res);


		} catch (ClientProtocolException e) {
			Toast.makeText(getApplicationContext(), "CPPe" + e.getMessage(), Toast.LENGTH_LONG);
			Log.e("PostActivity: ClientProtoException", e.getMessage());
			Log.e("PostActivity: ClientProtoException", e.getStackTrace().toString());
		} catch (IOException e) {
			Toast.makeText(getApplicationContext(), "CPPe" + e.getMessage(), Toast.LENGTH_LONG);
			Log.e("PostActivity: IoEx", e.getMessage());
			Log.e("PostActivity: IoEx", e.getStackTrace().toString());
		}
	}
	public String getCurrDate()
	{

		Date cal = Calendar.getInstance().getTime();
		String datetime = cal.toLocaleString();
		return datetime;
	}
	protected void showCurrentLocation() {

		Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		if (location != null) {
			latitude=Double.toString(location.getLatitude());
			longitude=Double.toString(location.getLongitude());

		}

	}   

	private class MyLocationListener implements LocationListener {

		public void onLocationChanged(Location location) {
			String message = String.format(
					"New Location \n Longitude: %1$s \n Latitude: %2$s",
					location.getLongitude(), location.getLatitude()
			);
			Toast.makeText(MyService.this, message, Toast.LENGTH_LONG).show();
		}

		public void onStatusChanged(String s, int i, Bundle b) {
			Toast.makeText(MyService.this, "Provider status changed",
					Toast.LENGTH_LONG).show();
		}

		public void onProviderDisabled(String s) {
			Toast.makeText(MyService.this,
					"Provider disabled by the user. GPS turned off",
					Toast.LENGTH_LONG).show();
		}

		public void onProviderEnabled(String s) {
			Toast.makeText(MyService.this,
					"Provider enabled by the user. GPS turned on",
					Toast.LENGTH_LONG).show();
		}

	}


	void streamend() throws IllegalArgumentException, IllegalStateException, IOException
	{
		//	serializer.text("stream ended at "+getCurrDate());
		serializer.endTag(null, "Logfile");
		serializer.endDocument();
		//write xml data into the FileOutputStream

		serializer.flush();
		//finally we close the file stream
		fileos.close();
		Log.d("file created","success");

	}
	void streamstart() throws IllegalArgumentException, IllegalStateException, IOException
	{

		try {
			//we set the FileOutputStream as output for the serializer, using UTF-8 encoding

			//i indent code just to have a view similar to xml-tree

			Log.d("process","process");
			for(; processcount < procInfos.size(); processcount++)
			{

				serializer.startTag(null, "process");
				++numberentry;
				serializer.attribute(serializer.getNamespace(), "ID",Integer.toString(processcount));
				processmap.put("process"+Integer.toString(processcount), procInfos.get(processcount).processName.toString());
				Log.d("name",procInfos.get(processcount).processName.toString());
				serializer.attribute(serializer.getNamespace(), "TIME STAMP",getCurrDate());						
				serializer.text(procInfos.get(processcount).processName);

				serializer.endTag(null, "process");
			}
			Log.d("processmap","finished");
			// Iterator myVeryOwnIterator = processmap.keySet().iterator();
			//while(myVeryOwnIterator.hasNext()) {
			//   String key=(String)myVeryOwnIterator.next();
			//  String value=(String)processmap.get(key);
			// Log.d("processmap",value);
			//}
			Log.d("processmap","finished2");
			Log.d("finished","first loop");
			for(; servicecount < l.size(); servicecount++){

				ActivityManager.RunningServiceInfo runningServiceInfo = (ActivityManager.RunningServiceInfo) l.get(servicecount);

				serializer.startTag(null, "Services");

				serializer.attribute(serializer.getNamespace(), "ID",Integer.toString(servicecount));
				serializer.attribute(serializer.getNamespace(), "TIME STAMP",getCurrDate());
				serializer.text(runningServiceInfo.service.getClassName());

				serializer.endTag(null, "Services");

				++numberentry;
			}


		} catch (Exception e) {
			Log.e("Exception","error occurred while creating xml file");
		}
	} 

	void fileupload(String path)
	{
		HttpURLConnection connection = null;
		DataOutputStream outputStream = null;
		@SuppressWarnings("unused")
		DataInputStream inputStream = null;

		String pathToOurFile = path;
		Log.d("upload",path);
		String urlServer = "http://172.16.100.162/test/upload/upload2.php";
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
		@SuppressWarnings("unused")
		int serverResponseCode = connection.getResponseCode();
		@SuppressWarnings("unused")
		String serverResponseMessage = connection.getResponseMessage();

		fileInputStream.close();
		outputStream.flush();
		outputStream.close();
		}
		catch (Exception ex)
		{
		//Exception handling
		}
	}
	public void getDeviceName() {
		manufacturer = Build.MANUFACTURER;
		model = Build.MODEL;

	} 
	void checknewprocess() throws IllegalArgumentException, IllegalStateException, IOException
	{	int i=1;
	int size=procInfos.size();
	Log.d("prcessmapsize",Integer.toString(size));
	while(i<size)
	{
		if (processmap.containsValue(procInfos.get(i).processName.toString()))
		{

		}
		else
		{
			serializer.startTag(null, "process");

			serializer.attribute(serializer.getNamespace(), "ID",Integer.toString(processcount));
			Log.d("new ","process");
			processmap.put("process"+Integer.toString(processcount), procInfos.get(i).processName.toString());
			Log.d("name",procInfos.get(i).processName.toString());
			serializer.attribute(serializer.getNamespace(), "TIME STAMP",getCurrDate());						

			serializer.text(procInfos.get(i).processName);

			++processcount;

			serializer.endTag(null, "process");
		}
		i++;
	}

	}

	void displaynotification()
	{
		notificationManager =(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);   
		myNotification = new Notification(R.drawable.icon,
				"Notification!",
				System.currentTimeMillis());
		Context context = getApplicationContext();
		String notificationTitle = "BITS TRACK APP NEW UPDATE";
		String notificationText = "visit 172.16.100.162/";
		Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(myBlog));
		PendingIntent pendingIntent
		= PendingIntent.getActivity(MyService.this,
				0, myIntent,
				Intent.FLAG_ACTIVITY_NEW_TASK);
		myNotification.defaults |= Notification.DEFAULT_SOUND;
		myNotification.flags |= Notification.FLAG_AUTO_CANCEL;
		myNotification.setLatestEventInfo(context,
				notificationTitle,
				notificationText,
				pendingIntent);
		notificationManager.notify(MY_NOTIFICATION_ID, myNotification);

	}
	public void appupdate(String s1) {
		// Create a new HttpClient and Post Header
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost("http://172.16.100.162/test/upload/test.php");

		try {
			// Add your data
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
			nameValuePairs.add(new BasicNameValuePair("currentversion", s1));


			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			// Execute HTTP Post Request
			HttpResponse response = httpclient.execute(httppost);
			String res=EntityUtils.toString(response.getEntity());
			Log.d("serverresponse",res);
			if(res.equals("update"))
			{
				displaynotification();
			}


		} catch (ClientProtocolException e) {
			Toast.makeText(getApplicationContext(), "CPPe" + e.getMessage(), Toast.LENGTH_LONG);
			Log.e("PostActivity: ClientProtoException", e.getMessage());
			Log.e("PostActivity: ClientProtoException", e.getStackTrace().toString());
		} catch (IOException e) {
			Toast.makeText(getApplicationContext(), "CPPe" + e.getMessage(), Toast.LENGTH_LONG);
			Log.e("PostActivity: IoEx", e.getMessage());
			Log.e("PostActivity: IoEx", e.getStackTrace().toString());
		}
	}
	public String wifi()
	{

		String network_type="UNKNOWN";//maybe usb reverse tethering
		NetworkInfo active_network=((ConnectivityManager)this.getSystemService(MyService.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
		Log.d("INwifi","hithere1");
		if (active_network!=null && active_network.isConnectedOrConnecting())
		{	Log.d("INwifi","hithere2");
			if (active_network.getType()==ConnectivityManager.TYPE_WIFI)
			{
				network_type="WIFI";
				Log.d("INwifi","hithere3");
				return network_type;
				
			}
			
		}
		Log.d("INwifi","hithere4");
		return "nowifi";
	}


}














