package org.pctechtips.netdroid;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.pctechtips.netdroid.classes.Node;

import java.util.ArrayList;


public class NodeAdapter extends ArrayAdapter<Node>{

    public NodeAdapter(Context context, int num, ArrayList<Node> allHost) {
        super(context, 0, allHost);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Node host = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(org.pctechtips.netdroid.R.layout.list_network, parent, false);
        }
        // Lookup view for data population
        ImageView nodeHostImgView = (ImageView) convertView.findViewById(R.id.host_icon);
        TextView nodeIpTxtView = (TextView) convertView.findViewById(org.pctechtips.netdroid.R.id.ip_address);
        TextView nodeMacTxtView = (TextView) convertView.findViewById(org.pctechtips.netdroid.R.id.mac_address);
        ImageView nodeArrowImgView = (ImageView) convertView.findViewById(org.pctechtips.netdroid.R.id.port_scan_arrow);
        // Populate the data into the template view using the data object
        nodeHostImgView.setImageResource(R.drawable.ic_computer_white_36dp);
        nodeIpTxtView.setText(host.getIp());
        nodeMacTxtView.setText(host.getMac());
        nodeArrowImgView.setImageResource(R.drawable.ic_keyboard_arrow_right_white_36dp);
        // Return the completed view to render on screen
        return convertView;
    }
}
