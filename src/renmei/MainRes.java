package renmei;

import in.wptrafficanalyzer.locationplacedetailsv2.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ParseException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public class MainRes extends FragmentActivity implements LocationListener{

	//private final LatLng LOCATION_Friends = new LatLng(40.8027171167234, -77.8578205013308);//change!
	GoogleMap mGoogleMap;	
	Spinner mSprPlaceType;	

	String[] mPlaceType=null;
	String[] mPlaceTypeName=null;

	static double mLatitude=40.8048496290786;
	static double mLongitude=-77.861618310732;

	HashMap<String, String> mMarkerPlaceLink = new HashMap<String, String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);		
		// Array of place types
		mPlaceType = getResources().getStringArray(R.array.place_type);
		// Array of place type names
		mPlaceTypeName = getResources().getStringArray(R.array.place_type_name);
		// Creating an array adapter with an array of Place types
		// to populate the spinner
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, mPlaceTypeName);
		// Getting reference to the Spinner 
		mSprPlaceType = (Spinner) findViewById(R.id.spr_place_type);
		// Setting adapter on Spinner to set place types
		mSprPlaceType.setAdapter(adapter);
		Button btnFind;
		// Getting reference to Find Button
		btnFind = ( Button ) findViewById(R.id.btn_find);
		// Getting Google Play availability status
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());
		if(status!=ConnectionResult.SUCCESS){ // Google Play Services are not available
			int requestCode = 10;
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
			dialog.show();
		}else { // Google Play Services are available
			// Getting reference to the SupportMapFragment
			SupportMapFragment fragment = ( SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
			// Getting Google Map
			mGoogleMap = fragment.getMap();
			// Enabling MyLocation in Google Map
			mGoogleMap.setMyLocationEnabled(true);
			// Getting LocationManager object from System Service LOCATION_SERVICE
			LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			// Creating a criteria object to retrieve provider
			Criteria criteria = new Criteria();
			// Getting the name of the best provider
			String provider = locationManager.getBestProvider(criteria, true);
			// Getting Current Location From GPS
			Location location = locationManager.getLastKnownLocation(provider);
			if(location!=null){
				onLocationChanged(location);
			}
			locationManager.requestLocationUpdates(provider, 20000, 0, this);
			// Setting click event lister for the find button
			btnFind.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {	
					int selectedPosition = mSprPlaceType.getSelectedItemPosition();
					String type = mPlaceType[selectedPosition];
					StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
					sb.append("location="+mLatitude+","+mLongitude);
					sb.append("&radius=5000");
					sb.append("&types="+type);
					sb.append("&sensor=true");
					sb.append("&key=AIzaSyDz2Vq-qd3a6TRPD8iLIosUiehwj5OZhqM");
					// Creating a new non-ui thread task to download Google place json data 
					PlacesTask placesTask = new PlacesTask();		        			     
					// Invokes the "doInBackground()" method of the class PlaceTask
					placesTask.execute(sb.toString());
				}
			});
		}		
	}

	private String downloadUrl(String strUrl) throws IOException{
		String data = "";
		InputStream iStream = null;
		HttpURLConnection urlConnection = null;
		try{
			URL url = new URL(strUrl);                
			// Creating an http connection to communicate with url 
			urlConnection = (HttpURLConnection) url.openConnection();                
			// Connecting to url 
			urlConnection.connect();                
			// Reading data from url 
			iStream = urlConnection.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
			StringBuffer sb  = new StringBuffer();
			String line = "";
			while( ( line = br.readLine())  != null){
				sb.append(line);
			}
			data = sb.toString();
			br.close();
		}catch(Exception e){
			Log.d("Exception while downloading url", e.toString());
		}finally{
			iStream.close();
			urlConnection.disconnect();
		}
		return data;
	}         

	private class PlacesTask extends AsyncTask<String, Integer, String>{
		String data = null;
		// Invoked by execute() method of this object
		@Override
		protected String doInBackground(String... url) {
			try{
				data = downloadUrl(url[0]);
			}catch(Exception e){
				Log.d("Background Task",e.toString());
			}
			return data;
		}
		// Executed after the complete execution of doInBackground() method
		@Override
		protected void onPostExecute(String result){			
			ParserTask parserTask = new ParserTask();
			// Start parsing the Google places in JSON format
			// Invokes the "doInBackground()" method of the class ParseTask
			parserTask.execute(result);
		}
	}

	private class ParserTask extends AsyncTask<String, Integer, List<HashMap<String,String>>>{
		JSONObject jObject;
		// Invoked by execute() method of this object
		@Override
		protected List<HashMap<String,String>> doInBackground(String... jsonData) {
			List<HashMap<String, String>> places = null;			
			PlaceJSONParser placeJsonParser = new PlaceJSONParser();
			try{
				jObject = new JSONObject(jsonData[0]);
				places = placeJsonParser.parse(jObject);
			}catch(Exception e){
				Log.d("Exception",e.toString());
			}
			return places;
		}
		// Executed after the complete execution of doInBackground() method
		@Override
		protected void onPostExecute(List<HashMap<String,String>> list){			
			// Clears all the existing markers 
			mGoogleMap.clear();
			for(int i=0;i<list.size();i++){
				// Creating a marker
				MarkerOptions markerOptions = new MarkerOptions();
				// Getting a place from the places list
				HashMap<String, String> hmPlace = list.get(i);
				// Getting latitude of the place
				double lat = Double.parseDouble(hmPlace.get("lat"));	            
				// Getting longitude of the place
				double lng = Double.parseDouble(hmPlace.get("lng"));
				// Getting name
				String name = hmPlace.get("place_name");
				// Getting vicinity
				String vicinity = hmPlace.get("vicinity");
				LatLng latLng = new LatLng(lat, lng);
				// Setting the position for the marker
				markerOptions.position(latLng);
				// Setting the title for the marker. 
				//This will be displayed on taping the marker
				markerOptions.title(name + " : " + vicinity);	            
				// Placing a marker on the touched position
				Marker m = mGoogleMap.addMarker(markerOptions);	            
				// Linking Marker id and place reference
				mMarkerPlaceLink.put(m.getId(), hmPlace.get("reference"));	            
			}		
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		mLatitude = location.getLatitude();
		mLongitude = location.getLongitude();
		String mLatitude2=Double.toString(mLatitude);
		String mLongitude2=Double.toString(mLongitude);
		Log.e(mLatitude2,mLongitude2);
		LatLng latLng = new LatLng(mLatitude, mLongitude);

		mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
		mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(12));

	}
	public void onClick_Me(View v) {
		//final LatLng LOCATION_Me = new LatLng(mLatitude, mLongitude);
		final LatLng LOCATION_Me = new LatLng(mLatitude, mLongitude);
		mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		CameraUpdate update = CameraUpdateFactory.newLatLngZoom(LOCATION_Me, 9);
		mGoogleMap.animateCamera(update);
		mGoogleMap.addMarker(new MarkerOptions().position(LOCATION_Me).title("Find me here!").icon(BitmapDescriptorFactory.fromResource(R.drawable.pin_me)));
	}
	public void onClick_Friends(View v) {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy); 
		//connect to php and json parse
		String result = null;
		InputStream is = null;
		JSONArray jArray = null;
		ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		//nameValuePairs.add(new BasicNameValuePair("ID",1003));
		try
		{
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost("http://10.0.2.2/516/select2.php");
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse response = httpclient.execute(httppost); 
			HttpEntity entity = response.getEntity();
			is = entity.getContent();

			Log.e("log_tag", "connection success ");

		}
		catch(Exception e)
		{
			Log.e("log_tag", "Error in http connection "+e.toString());
			Toast.makeText(getApplicationContext(), "Connection fail", Toast.LENGTH_SHORT).show();

		}
		//convert response to string
		try{
			BufferedReader reader = new BufferedReader(new InputStreamReader(is,"iso-8859-1"),8);
			StringBuilder sb = new StringBuilder();
			sb.append(reader.readLine() + "\n");

			String line="0";
			while ((line = reader.readLine()) != null) 
			{
				sb.append(line + "\n");

			}
			is.close();

			result=sb.toString();
		}
		catch(Exception e)
		{
			Log.e("log_tag", "Error converting result "+e.toString());

			Toast.makeText(getApplicationContext(), " Input reading fail", Toast.LENGTH_SHORT).show();

		}
		//parse json data
		try{
			jArray = new JSONArray(result);
			JSONObject json_data=null;            
			for(int i=0;i<jArray.length();i++){
				json_data = jArray.getJSONObject(i);
				String username = json_data.getString("username");//here "Name" is the column name in database
				String phone = json_data.getString("phone");
				String comments = json_data.getString("comments");
				Double w = json_data.getDouble("loc_lat");
				Double e = json_data.getDouble("loc_long");
				String w2=Double.toString(w);
				String e2=Double.toString(e);
				Log.e(w2,e2);
				mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
				final LatLng LOCATION_Friends2 = new LatLng(w,e);
				CameraUpdate update = CameraUpdateFactory.newLatLngZoom(LOCATION_Friends2, 9);
				mGoogleMap.animateCamera(update);
				mGoogleMap.addMarker(new MarkerOptions().position(LOCATION_Friends2).title(username+","+phone+","+comments) .icon(BitmapDescriptorFactory.fromResource(R.drawable.pin)));
				//Log.e(myString1,myString2);

			}
		}
		catch(JSONException e1){
			Toast.makeText(getBaseContext(), "No Data Found" ,Toast.LENGTH_LONG).show();
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
	}

	public void onClick_SMS(View v) {
		try {

			Intent smsIntent = new Intent(Intent.ACTION_VIEW);
			smsIntent.putExtra("sms_body", "Hi, there!");
			smsIntent.setType("vnd.android-dir/mms-sms");
			startActivity(smsIntent);

		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), "SMS faild!",
					Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}
	public void onClick_Login(View v) {
		Intent i = new Intent(getApplicationContext(),insert.class);
		startActivity(i);
		setContentView(R.layout.insert);

	}
	public void onClick_Logout(View v) {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy); 
		//connect to php and json parse
		String result = null;
		InputStream is = null;
		ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		//String v1="jen";
		String mylat = Double.toString(mLatitude); //change to mLatitude
		nameValuePairs.add(new BasicNameValuePair("loc_lat",mylat));
		//http post
		try
		{
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost("http://10.0.2.2/516/delete.php");
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse response = httpclient.execute(httppost); 
			HttpEntity entity = response.getEntity();
			is = entity.getContent();

			Log.e("log_tag", "connection success ");
			Toast.makeText(getApplicationContext(), "pass", Toast.LENGTH_SHORT).show();
		}
		catch(Exception e)
		{
			Log.e("log_tag", "Error in http connection "+e.toString());
			Toast.makeText(getApplicationContext(), "Connection fail", Toast.LENGTH_SHORT).show();

		}
		//convert response to string
		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(is,"iso-8859-1"),8);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			is.close();

			result=sb.toString();
		}
		catch(Exception e)
		{
			Log.e("log_tag", "Error converting result "+e.toString());

		}
		//parse json data
		try
		{

			JSONObject json_data = new JSONObject(result);


			CharSequence w= (CharSequence) json_data.get("re");

			Toast.makeText(getApplicationContext(), w, Toast.LENGTH_SHORT).show();

		}
		catch(JSONException e)
		{
			Log.e("log_tag", "Error parsing data "+e.toString());
			Toast.makeText(getApplicationContext(), "JsonArray fail", Toast.LENGTH_SHORT).show();
		}
	}
	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub		
	}
}