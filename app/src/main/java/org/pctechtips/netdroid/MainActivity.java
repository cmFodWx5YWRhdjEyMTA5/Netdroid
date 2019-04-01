package org.pctechtips.netdroid;

/*
* IpSCanner and PortScanner App for Android
* */

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.InterfaceAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import com.google.android.gms.ads.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {

    private final static String INTERNET_IP_SOURCE = "https://ipinfo.io/json";
    TextView txtWifiName;
    ArrayList<String> localIfaceInfo; //holds all local interface config
    ArrayList<String> allInterfaceAdd; //all interface 
    public String ip = null;
    Toolbar myToolbar;
    private PopupWindow popUpWindow;
    private LinearLayout linearMain;
    private LinearLayout linearWifiName;
    WifiManager wifiMan;
    WifiInfo wifiInfo;
    TaskPublicNet taskPublicNet;
    private ImageView ImgWifiIcon;
    private ProgressBar progressBar;
    private DhcpInfo dhcpInfo;
    private int cidr;
    private AdView mAdView;
    private AdRequest adRequest;
    private ListView localNetListView;
    private org.pctechtips.netdroid.HostAdapter localNetAdapter;
    private ImageView scanButton;
    private ConnectivityManager cm;
    private NetworkInfo info;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(org.pctechtips.netdroid.R.layout.activity_main);

        /* initilize ads app-id*/
        MobileAds.initialize(getApplicationContext(), "ca-app-pub-3940256099942544~3347511713");
//        MobileAds.initialize(getApplicationContext(), "ca-app-pub-6077312877853191~2989221860");
        mAdView = (AdView) findViewById(R.id.adView_main);
        adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //setting the ActionBar for the activity
        myToolbar = (Toolbar) findViewById(org.pctechtips.netdroid.R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        localIfaceInfo = new ArrayList<>();
        allInterfaceAdd = new ArrayList<>();

        //referencing views
        ImgWifiIcon = (ImageView) findViewById(org.pctechtips.netdroid.R.id.wifi_icon_id);
        progressBar = (ProgressBar) findViewById(org.pctechtips.netdroid.R.id.progress_bar);
        txtWifiName = (TextView) findViewById(org.pctechtips.netdroid.R.id.wifi_name);
        //scanButton = (ImageView) findViewById(R.id.scan_button);


        linearWifiName = (LinearLayout) findViewById(org.pctechtips.netdroid.R.id.linear_wifi_name);
        //referencing LinearLayout in activity_main.xml for popup window
        linearMain = (LinearLayout) findViewById(org.pctechtips.netdroid.R.id.linearLayout_main);

        cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        info = cm.getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            txtWifiName.setText(info.getExtraInfo().replace("\"", "").toUpperCase());
            Log.v("SSID", info.getExtraInfo());
        }

        wifiMan = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        wifiInfo = wifiMan.getConnectionInfo();
        Log.v("WIFIINFO:", wifiInfo.getSSID());
        dhcpInfo = wifiMan.getDhcpInfo();


        Log.v("android ver", getAndroidVersion());

        /*getting subnet mask private net*/
        getNetInterfaceCfg();

        taskPublicNet = new TaskPublicNet();
        taskPublicNet.execute();

        //inflating adapter
        localNetListView  = (ListView) findViewById(R.id.local_network);
        localNetAdapter = new org.pctechtips.netdroid.HostAdapter(this, R.layout.list_main, localIfaceInfo);
        localNetListView.setAdapter(localNetAdapter);

        /*
         * if wifi adapter is not enabled or phone not connected to wifi
         * change wifi image icon to no connection. and display message
         */
        /*if (wifiMan.isWifiEnabled() || wifiInfo.getSupplicantState().equals(SupplicantState.COMPLETED)) {
            //setting wifi name to access point name
            txtWifiName.setText(wifiInfo.getSSID().replace("\"", "").toUpperCase());
        }
        else {
            txtWifiName.setText("CONNECT TO WIFI");
            //setting no connection icon and intent to wifi list in Android settings
            ImgWifiIcon.setImageResource(org.pctechtips.netdroid.R.drawable.ic_signal_wifi_off_white_36dp);
        }*/

        /*
        * listener for LinearLayout WifiName: create intent depending of wifi connection state
        * if wifi is connected to AccessPoint then scan, if not send user to connect first!
        */
        /*scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(wifiInfo.getSupplicantState().equals(SupplicantState.COMPLETED)) {
                    Intent ipScanIntent = new Intent(getApplicationContext(), IpScanActivity.class);
                    ipScanIntent.putExtra("ip", ip);
                    ipScanIntent.putExtra("cidr", cidr);
                    startActivity(ipScanIntent);
                }
                else {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                }
            }
        });*/
    }

    /*
    * method to convert ip from int to string format
    */
    private String getIpToString(int ip) {
        //getting local ip and converting from hex to decimal
        //int ipAddress = wifiInfo.getIpAddress();
        return String.format("%d.%d.%d.%d",
                (ip & 0xff),
                (ip >> 8 & 0xff),
                (ip >> 16 & 0xff),
                (ip >> 24 & 0xff));
    }

    /*
    * calculating cidr notation of subnet IPv4
    */
    public void getNetInterfaceCfg() {
        Enumeration<NetworkInterface> networkInterfaces = null;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            try {
                /*
                * adding to localIfaceInfo arraylist all interface
                * and calculating cidr for ipv4 address
                */
                for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                    //getting local ipv4 interface info
                    if (address.getBroadcast() != null) {
                        ip = address.getAddress().getHostAddress();
                        cidr = address.getNetworkPrefixLength();
                        allInterfaceAdd.add("ipv4" + address.toString());
                        localIfaceInfo.add("IPv4: " + ip + "/" + cidr);
                        //eliminating / in /192.0.0.12 so addr is 192.0.0.12
                        localIfaceInfo.add("Broadcast: " + address.getBroadcast().toString().replaceAll("^/", ""));
                        continue;
                    }
                    allInterfaceAdd.add(address.toString());
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * method to get android software version information
     * @return sdkVersion, release
     */
    public String getAndroidVersion() {
        String release = Build.VERSION.RELEASE;
        int sdkVersion = Build.VERSION.SDK_INT;
        return sdkVersion + " (" + release +")";
    }


    /*
    * This AsyncTask needs to run in background.
    * get public ip / network information
    */
    private class TaskPublicNet extends AsyncTask<Void, String, Void> {
        String output = "";
        int progress = 0;
        String strRegex = "\""; //removing quotes
        //only 7 lines of information for public ip info (set progress at 5)
        final static int PROGRESS_MAX = 5;
        String[] iface;

        @Override
        protected void onPreExecute() {
            progressBar.setMax(PROGRESS_MAX);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(1);

            /* populating localIfaceInfo array with local
            * ip and wifi information */
            Log.v("WIFI:", wifiInfo.getBSSID() +" "+ wifiInfo.getSSID());
            localIfaceInfo.add("DNS: " + getIpToString(dhcpInfo.dns1));
            localIfaceInfo.add("Gateway: " + getIpToString(dhcpInfo.gateway));
            localIfaceInfo.add("Signal: " + wifiInfo.getRssi() + "db");
            localIfaceInfo.add("Speed: " + wifiInfo.getLinkSpeed() + "Mbps");
            localIfaceInfo.add("SSID: " + info.getExtraInfo().replace("\"", ""));
            localIfaceInfo.add("BSSID: " + wifiInfo.getBSSID());



        }

        @Override
        protected void onPostExecute(Void aVoid) {
            progressBar.setProgress(5);
            Log.v("ALL ", localIfaceInfo.toString());

        }

        @Override
        protected void onProgressUpdate(String... values) {
            Log.v("PROGRESS ", values[0]);
            localIfaceInfo.add(values[0]);
            localNetAdapter.notifyDataSetInvalidated();

        }

        @Override
        protected Void doInBackground(Void... params) {

            /** Getting public interface information*/
            try {
                // Make a URL to the web page
                URL url = new URL(INTERNET_IP_SOURCE);
                // Get the input stream through URL Connection
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                BufferedReader bf = new BufferedReader(new InputStreamReader(inputStream));
                String line = "";
                // read each line and write to System.out
                while ((line = bf.readLine()) != null) {
                    Log.v("LINE ", line);
                    output += line;
                }
                bf.close();
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            //json parsing
            try {

//                JSONArray ja = new JSONArray(output);
                JSONObject jo = new JSONObject(output);
                String publicIp = jo.get("ip").toString().replaceAll(strRegex, "");
                String city = jo.get("city").toString().replaceAll(strRegex ,"");
                String country = jo.get("country").toString().replaceAll(strRegex, "");
//                String state = jo.get("region").toString().replaceAll(strRegex, "");
                String hostname = jo.get("hostname").toString().replaceAll(strRegex, "");
                Log.v("PUBLIC ", publicIp +" "+city+" "+country);
                publishProgress("Public Network")
                publishProgress("PublicIp: "+publicIp);
                publishProgress("Hostname: "+hostname);
                publishProgress("City: "+city);
                publishProgress("Country: "+country);
            }
            catch (JSONException ex){
                ex.printStackTrace();
            }
            return null;
        }
    }


    /*
    * Infating the menu for toolbar
    */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(org.pctechtips.netdroid.R.menu.menu, menu);
        return true;
    }

    /*
    * actions for menu options in toolbar
    */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == org.pctechtips.netdroid.R.id.action_ping) {
            Intent pingIntent = new Intent(MainActivity.this, PingActivity.class);
            startActivity(pingIntent);
            return true;
        }

        if (id == org.pctechtips.netdroid.R.id.action_netstat) {
            Intent netstatIntent = new Intent(this, NetstatActivity.class);
            startActivity(netstatIntent);
            return true;
        }

        //Rate this app on Google play
        if (id == org.pctechtips.netdroid.R.id.app_ratings) {
            Uri uri = Uri.parse("market://details?id=" + this.getPackageName());
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
            // To count with Play market backstack, After pressing back button,
            // to taken back to our application, we need to add following flags to intent.
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            try {
                startActivity(goToMarket);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=" + this.getPackageName())));
            }
            return true;
        }
        if (id == org.pctechtips.netdroid.R.id.action_refresh) {
            //reload this activity (refresh)
            Intent thisActivity = new Intent(this, MainActivity.class);
            startActivity(thisActivity);
            return true;
        }

        if (id == org.pctechtips.netdroid.R.id.action_about) {
            showAboutWindow();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
    * method to display about popup window with app informmation
    * eg: developer name, version, website
    */
    public void showAboutWindow() {
        LayoutInflater layoutInflater
                = (LayoutInflater)getBaseContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = layoutInflater.inflate(org.pctechtips.netdroid.R.layout.popup_win, null);
        popUpWindow = new PopupWindow(
                popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        Button btnDismiss = (Button)popupView.findViewById(org.pctechtips.netdroid.R.id.dismiss);
        btnDismiss.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                popUpWindow.dismiss();
            }});

        //position popup window at center of screen
        popUpWindow.showAtLocation(linearMain, Gravity.CENTER, 0, 0);
    }

}