package org.pctechtips.netdroid;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class PingActivity extends AppCompatActivity {

    TextView statusMsg;
    ArrayList<String> pingResult;
    ListView listViewPing;
    Toolbar myToolbar;
    PingAdapter pingAdapter;
    EditText hostIp;
    private TaskPingHost pingHost;
    Button pingBtn;
    private AdView mAdView;
    private AdRequest adRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(org.pctechtips.netdroid.R.layout.ping);

        /* initilize ads app-id*/
        /*MobileAds.initialize(getApplicationContext(), "ca-app-pub-3940256099942544~3347511713");*/
        MobileAds.initialize(getApplicationContext(), "ca-app-pub-6077312877853191~2989221860");
        mAdView = (AdView) findViewById(R.id.adView_ping);
        adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //setting the ActionBar for the activity
        myToolbar = (Toolbar) findViewById(org.pctechtips.netdroid.R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        //adding back arrow to toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        statusMsg = (TextView) findViewById(org.pctechtips.netdroid.R.id.status_msg);

        pingResult = new ArrayList<String>();
        hostIp = (EditText) findViewById(org.pctechtips.netdroid.R.id.host_ip); //getting hostname/ip address to ping
        listViewPing = (ListView) findViewById(org.pctechtips.netdroid.R.id.ping_output);

        //inflating the adapter
        pingAdapter = new PingAdapter(this, R.layout.list_ping, pingResult);
        listViewPing.setAdapter(pingAdapter);

        //click event listener for ping button
        pingBtn = (Button) findViewById(org.pctechtips.netdroid.R.id.ping_btn);
        pingBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //stargin asynctask ping host
                pingHost = new TaskPingHost();
                pingHost.execute();
            }
        });
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
            Intent thisActivity = new Intent(this, PingActivity.class);
            startActivity(thisActivity);
            return true;
        }

        if (id == org.pctechtips.netdroid.R.id.action_about) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
    * {@TaskPingHost} class will ping host by ip or hostname
    */
    private class TaskPingHost extends AsyncTask<Void, String, Void> {

        String pingCmd = "/system/bin/ping -c 10 ";
        String host;
        String output[];

        @Override
        protected void onPreExecute() {
            //clear arraylist
            pingResult.clear();
            host = hostIp.getText().toString();
            //putting ping command together
            pingCmd = pingCmd + host;
        }

        @Override
        protected Void doInBackground(Void... params) {
            //calling ping command from android bash
            try {
                Process p = Runtime.getRuntime().exec(pingCmd);
                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    publishProgress(inputLine);
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            //only add to ArrayList<Node> if host is alive on network
            if(values[0] != null && values[0].length() > 0) {
                //parsing the bytes response from ping
                if(values[0].contains("bytes") && !values[0].contains("PING")){ //parsing the packets of ping output
                    //updatingn packets from ping command
                    pingResult.add(values[0]);
                    pingAdapter.notifyDataSetInvalidated();
                }
            }
        }
    }
}
