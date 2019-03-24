package org.pctechtips.netdroid;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import org.pctechtips.netdroid.dbhelper.DatabaseHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by jlvaz on 3/7/2017.
 */

public class PortScanActivity extends AppCompatActivity {

    String ipAddress;
    ArrayList<String> openPorts;
    private org.pctechtips.netdroid.PortAdapter portAdapter;
    Toolbar myToolbar;
    TextView statusMsg;
    ProgressBar progressBar;
    private PopupWindow popUpWindow;
    private LinearLayout linearListPort;
    private ImageView scanListImg;
    private TextView portNumTxtView;
    private TextView portOpenTxtView;
    private TextView scanLabelTxtView;
    private DatabaseHelper portsDB;
    private AdView mAdView;
    private AdRequest adRequest;
    private  ListView portList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(org.pctechtips.netdroid.R.layout.scan_list);

        /* initilize ads app-id*/
        /*MobileAds.initialize(getApplicationContext(), "ca-app-pub-3940256099942544~3347511713");*/
        MobileAds.initialize(getApplicationContext(), "ca-app-pub-6077312877853191~2989221860");
        mAdView = (AdView) findViewById(R.id.adView_ip_scan);
        adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        /*reference and inflate custom toolbar*/
        myToolbar = (Toolbar) findViewById(org.pctechtips.netdroid.R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        //adding back arrow to toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Intent portIntent = getIntent();
        ipAddress = portIntent.getStringExtra("host");

        /*
        * creating reference, copy, and open database
        */
        portsDB = new DatabaseHelper(this);
        try {
            portsDB.createDataBase();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
        * reference views for scan_list.xml
        */
        statusMsg = (TextView) findViewById(org.pctechtips.netdroid.R.id.status_msg);
        progressBar = (ProgressBar) findViewById(org.pctechtips.netdroid.R.id.progress_bar);
        scanListImg = (ImageView) findViewById(org.pctechtips.netdroid.R.id.scan_list_icon);
        scanListImg.setImageResource(org.pctechtips.netdroid.R.drawable.ic_search_white_36dp);

        /* labels for port scan results (port / type)*/
        portNumTxtView = (TextView) findViewById(R.id.host_port_label);
        portOpenTxtView = (TextView) findViewById(R.id.ip_open_label);
        scanLabelTxtView = (TextView) findViewById(R.id.scan_label);
        portNumTxtView.setText("Port");
        portOpenTxtView.setText("State / Service");
        scanLabelTxtView.setText(""); //hiding this label for port scanning


        //referencing LinearLayout in activity_main.xml for popup window
        linearListPort = (LinearLayout) findViewById(org.pctechtips.netdroid.R.id.scan_list_linear);

        //setting the adapter
        portList = (ListView) findViewById(R.id.scan_list);
        openPorts = new ArrayList<>();
        portAdapter = new org.pctechtips.netdroid.PortAdapter(this, R.layout.list_port, openPorts);
        portList.setAdapter(portAdapter);

        //scanning ports
        PortScanTask portScan = new PortScanTask();
        portScan.execute();

        progressBar.setClickable(false);
    }

    /*
    * Inflating the menu for toolbar
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

        if (id == org.pctechtips.netdroid.R.id.action_refresh) {
            //reload this activity (refresh)
            Intent portIntent = getIntent();
            portIntent.getStringExtra("host");
            startActivity(portIntent);
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
        popUpWindow.showAtLocation(linearListPort, Gravity.CENTER, 0, 0);
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

    private class PortScanTask extends AsyncTask<Void, String, Void> {

        final int NUM_OF_PORTS = 1024; //well known ports
        final int NUM_OF_THREADS = (NUM_OF_PORTS / 4); //256 threads
        ExecutorService executor = Executors.newFixedThreadPool(NUM_OF_THREADS);
        //Thread[] thread = new Thread[NUM_OF_THREADS];
        org.pctechtips.netdroid.PortScanRunnable[] task = new org.pctechtips.netdroid.PortScanRunnable[NUM_OF_THREADS];
        int startPort = 0;
        int endPort = (NUM_OF_PORTS / NUM_OF_THREADS); //end port for every thread - 4
        int progress = 0;


        @Override
        protected void onPreExecute() {
            openPorts.clear();
            statusMsg.setText("Scanning ports..");
            progressBar.setMax(NUM_OF_PORTS);
            progressBar.setProgress(100);
        }

        @Override
        protected Void doInBackground(Void... params) {
             /* starging threads and adjusting port range - 4 ports by every thread */
            for(int i = 0; i < NUM_OF_THREADS; i++ ) {
                task[i] = new org.pctechtips.netdroid.PortScanRunnable(ipAddress, startPort, endPort, portsDB);
//                thread[i] = new Thread(task[i]);
                executor.execute(task[i]);
                startPort = endPort+1;
                endPort = startPort + (NUM_OF_PORTS / NUM_OF_THREADS); //4 ports
            }

            executor.shutdown();
            try {
                executor.awaitTermination(3, TimeUnit.SECONDS);
                executor.shutdownNow();
            } catch (InterruptedException e) {
                return null;
            }

//            publishProgress();

            /* starting threads */
            /*for(int i = 0; i < NUM_OF_THREADS; i++) {
                thread[i].start();
            }

            //stopping threads
            for(int i = 0; i < NUM_OF_THREADS; i++) {
                try {
                    thread[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }*/

            //collecting data from all threads
            for(int i = 0; i < NUM_OF_THREADS; i++) {
                for(String port : task[i].ports) {
                    publishProgress(port);
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            //only add to
            if(values[0] != null) {
                openPorts.add(values[0]);
                portAdapter.notifyDataSetInvalidated();
            }

            progressBar.setProgress(progress++);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            statusMsg.setText(ipAddress + " Ports");
            progressBar.setProgress(NUM_OF_PORTS);
        }
    }
}

