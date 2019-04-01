package org.pctechtips.netdroid;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import org.pctechtips.netdroid.classes.Node;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by jlvaz on 3/1/2017.
 */

public class IpScanActivity extends AppCompatActivity {

    static String ip;
    ArrayList<Node> hostList;
    ListView scanList;
    org.pctechtips.netdroid.NodeAdapter networkAdapter;
    TextView statusMsg;
    private TextView ipLabelTxtView;
    private TaskScanNetwork scanNetwork;
    private ProgressBar scanProgress;
    Toolbar myToolbar;
    private PopupWindow popUpWindow;
    private LinearLayout linearListIp;
    static private int numOfHost;
    static private int cidr;
    private AdView mAdView;
    private AdRequest adRequest;
    private static final String ARP_TABLE = "/proc/net/arp";
    private static final String ARP_FLAG = "0x2";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(org.pctechtips.netdroid.R.layout.scan_list);

        /* initilize adMob app-id*/
        MobileAds.initialize(getApplicationContext(), "ca-app-pub-3940256099942544~3347511713");
//        MobileAds.initialize(getApplicationContext(), "ca-app-pub-6077312877853191~2989221860");
        mAdView = (AdView) findViewById(R.id.adView_ip_scan);
        adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //setting the ActionBar for the activity
        myToolbar = (Toolbar) findViewById(org.pctechtips.netdroid.R.id.my_toolbar);
        myToolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(myToolbar);
        //adding back arrow to toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //getting ip addres from mainActivity
        Intent ipScanIntent = getIntent();
        ip = ipScanIntent.getStringExtra("ip");
        cidr = ipScanIntent.getIntExtra("cidr", 0);

        Log.v("SCAN", "scan avtivity");

        hostList = new ArrayList<>();
        statusMsg = (TextView) findViewById(org.pctechtips.netdroid.R.id.status_msg);
        scanProgress = (ProgressBar) findViewById(org.pctechtips.netdroid.R.id.progress_bar);
        /* labels for ip scan results IPs and MACs*/
        ipLabelTxtView = (TextView) findViewById(R.id.ip_open_label);
        ipLabelTxtView.setText("IP / Mac");

        //referencing LinearLayout in activity_main.xml for popup window
        linearListIp = (LinearLayout) findViewById(org.pctechtips.netdroid.R.id.scan_list_linear);

        //inflating adapter
        scanList  = (ListView) findViewById(org.pctechtips.netdroid.R.id.scan_list);
        networkAdapter = new org.pctechtips.netdroid.NodeAdapter(this, org.pctechtips.netdroid.R.layout.list_network, hostList);
        scanList.setAdapter(networkAdapter);

        getNumOfHost();

        //scanning network
        scanNetwork = new TaskScanNetwork();
        scanNetwork.execute();

        //setting the listeners for individuals ip address to start port scan
        scanList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> paren, View view, int position, long id) {
                //cancel AsyncTask if item in list is clicked
                scanNetwork.cancel(true);
                //start PortScanActivity, passing ipAddress position, and passing extras,
                Node host = hostList.get(position);
                Intent portScanIntent = new Intent(IpScanActivity.this, PortScanActivity.class);
                portScanIntent.putExtra("host", host.getIp());
                startActivity(portScanIntent);
            }
        });
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
            Intent pingIntent = new Intent(this, PingActivity.class);
            startActivity(pingIntent);
            return true;
        }

        if (id == org.pctechtips.netdroid.R.id.action_netstat) {
            Intent netstatIntent = new Intent(this, NetstatActivity.class);
            startActivity(netstatIntent);
            return true;
        }

        //reload this activity (refresh)
        if (id == org.pctechtips.netdroid.R.id.action_refresh) {
            Intent ipScanIntent = getIntent();
            ipScanIntent.getStringExtra("ip");
            ipScanIntent.getIntExtra("cidr", 0);
            startActivity(ipScanIntent);
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
        popUpWindow.showAtLocation(linearListIp, Gravity.CENTER, 0, 0);
    }

    /*
   * this method will handle action to back arrow in toolbar.
   * send to previous activity.
   */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /*
    * calculating the max number of host in network
    * based on subnet cidr. first get how many bits
    * are left for host portion then (2 power of hostBits)
    */
    public void getNumOfHost() {
        final int MAX_ADDR_BITS = 32;
        double hostBits = MAX_ADDR_BITS - cidr;
        numOfHost = (int) (Math.pow(2.0, hostBits) - 2);
    }


    /*
    * AscynTask to scan the network
    * you should try different timeout for your network/devices
    * it will try to detect localhost ip addres and subnet. then
    * it will use subnet to scan network
    */
    private class TaskScanNetwork extends AsyncTask<Void, Void, Void> {
        static final int NUMTHREADS = 254;
        String subnet = ip.substring(0, ip.lastIndexOf("."));
        int startIp = 1;
        int range = (numOfHost / NUMTHREADS); //range to be scanned by every thread
        int stopIp = startIp + range;
        org.pctechtips.netdroid.IpScanRunnable[] task = new org.pctechtips.netdroid.IpScanRunnable[NUMTHREADS];
//        Thread[] thread = new Thread[NUMTHREADS];
        ExecutorService executor = Executors.newFixedThreadPool(NUMTHREADS);
        InetAddress ia;


        @Override
        protected void onPreExecute() {
            hostList.clear();
            scanProgress.setMax(numOfHost);
            scanProgress.setProgress((numOfHost * 10) / 100 ); //set progress bar at 10%
            statusMsg.setText("Scanning " + subnet + ".0/" + cidr);
        }

        /* initialaze threads */
        @Override
        protected Void doInBackground(Void... params) {
            Log.v("BACKGROUND", "doInBackground stuff");
            for(int i = 0; i < NUMTHREADS; i++) {
                task[i] = new org.pctechtips.netdroid.IpScanRunnable(subnet, startIp, stopIp);
                executor.execute(task[i]);
                startIp = stopIp;
                stopIp = startIp + range;
            }

            executor.shutdown();

            try {
                executor.awaitTermination(3, TimeUnit.SECONDS);
                executor.shutdownNow();
            } catch (InterruptedException e) {
                return null;
            }

            publishProgress();
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... params) {
            //update progress bar
            /*scanProgress.setProgress(values[0].progressBar);*/
            try {

                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ARP_TABLE), "UTF-8"));
                reader.readLine(); // Skip header.
                String line;

                while ((line = reader.readLine()) != null) {
                    Log.v("ARPFILE", line);
                    String[] arpLine = line.split("\\s+");
                    //if arp line contains flag 0x2 we parse host
                    if(arpLine[2].equals(ARP_FLAG)) {
                        final String ip = arpLine[0];
                        final String flag = arpLine[2];
                        final String mac = arpLine[3];
//                        ia = InetAddress.getByName(ip);
//                        Log.v("PARSED", ip +" "+flag+" "+mac + " "+ ia.getCanonicalHostName());
                        Node node = new Node(ip, mac);
                        hostList.add(node);
                        networkAdapter.notifyDataSetInvalidated();
//                        scanProgress.setProgress(node.progressBar);
                    }
                }
            }
            catch (Exception ex) {

            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            statusMsg.setText(subnet + ".0/24 " + " Hosts");
            scanProgress.setProgress(254);
        }
    }

}
