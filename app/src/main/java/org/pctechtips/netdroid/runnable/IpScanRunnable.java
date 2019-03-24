package org.pctechtips.netdroid;

import android.util.Log;

import org.pctechtips.netdroid.classes.Node;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jlvaz on 3/29/2017.
 */

public class IpScanRunnable implements Runnable {
    private List<Node> results;
    private static final String TAG = "IPSCANRUNNABLE";
    private final static int TIMEOUT = 1000;
    private final static int PORT = 7;

    private String subnet;
    private Integer startIp;
    private Integer stopIp;

    public IpScanRunnable(String subnet, int start, int stop) {
        this.subnet = subnet;
        this.startIp = start;
        this.stopIp = stop;
        results = new ArrayList();

    }

    /**
     * populating /proc/net/arp file by establish connection
     * to every host on the network using socket
     */
    @Override
    public void run() {
        Socket socket = null;
        for(int i = startIp; i < stopIp; i++) {
            String ip = subnet + "." + i;
            socket = new Socket();
            try {
                InetAddress ipAdd = InetAddress.getByName(ip);
                byte[] ipBytes = ipAdd.getAddress();
                socket.setTcpNoDelay(true);
                socket.connect(new InetSocketAddress(InetAddress.getByAddress(ipBytes), PORT), TIMEOUT);
            }
            catch (Exception ex){

            }
        }
    }


    public List<Node> getResults() {
        return results;
    }
}

