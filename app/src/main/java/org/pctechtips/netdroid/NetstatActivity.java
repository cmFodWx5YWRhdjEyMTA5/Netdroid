package org.pctechtips.netdroid;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by jlvaz on 3/19/2017.
 * NetstatActivity for displaying localhost connections
 */

public class NetstatActivity extends AppCompatActivity {

    ArrayList<String> localConnections;
    ListView listViewConnect;
    Toolbar myToolbar;
    private TaskNetstat taskNetstat;
    private AdView mAdView;
    private AdRequest adRequest;
    ListView netstatList;
    NetstatAdapter netstatAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(org.pctechtips.netdroid.R.layout.netstat);

        /* initilize ads app-id*/
        MobileAds.initialize(getApplicationContext(), "ca-app-pub-3940256099942544~3347511713");
        /*MobileAds.initialize(getApplicationContext(), "ca-app-pub-6077312877853191~9484910663");*/
        mAdView = (AdView) findViewById(R.id.adView_netstat);
        adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //setting the ActionBar for the activity
        myToolbar = (Toolbar) findViewById(org.pctechtips.netdroid.R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        //adding back arrow to toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        localConnections = new ArrayList<String>();
        listViewConnect = (ListView) findViewById(org.pctechtips.netdroid.R.id.netstat_list);

        //inflating the adapter
        //setting the adapter
        netstatList = (ListView) findViewById(R.id.netstat_list);
        netstatAdapter = new NetstatAdapter(this, R.layout.list_stat, localConnections);
        netstatList.setAdapter(netstatAdapter);

        taskNetstat = new TaskNetstat();
        taskNetstat.execute();
    }

   /*
    * method to display local connections with netstat -ant command
    * and add to ArrayList localConnections
    */

    private class TaskNetstat extends AsyncTask<Void, String, Void> {
        String netstatCmd = "/system/bin/netstat -ant";

        @Override
        protected void onProgressUpdate(String... values) {
            if (values[0] != null && !values[0].contains("connections")) {
                localConnections.add(values[0]);
                netstatAdapter.notifyDataSetChanged();
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Runtime r = Runtime.getRuntime();
                Process p = r.exec(netstatCmd);
                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String inputLine;
                /* parsing output of netstat cmd
                * protocol, remote addr:port, state*/
                while ((inputLine = in.readLine()) != null) {
                    if(!inputLine.contains("tcp")) {
                        continue;
                    }
                    publishProgress(inputLine);
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
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
            Intent thisActivity = new Intent(this, NetstatActivity.class);
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

}
