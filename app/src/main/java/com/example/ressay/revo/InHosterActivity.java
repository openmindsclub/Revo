package com.example.ressay.revo;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Iterator;

public class InHosterActivity extends AppCompatActivity {

    ListView myList;
    TextView hostName;
    NetworkScanHost.Host host = null;
    String[] players = new String[0];
    Activity act = this;
    NetworkManager net = NetworkManager.getInstance();
    HostListener listener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_hoster);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        myList = (ListView) findViewById(R.id.list);
        hostName = (TextView) findViewById(R.id.hostName);
        host = ScanActivity.host;
        hostName.setText(host.getName());
    }

    @Override
    public void onResume()
    {
        super.onResume();
        net.sendMessageToHost("connect:"+MainActivity.getNickName(),host);
        listener = new HostListener();
        listener.execute();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        listener.cancel(true);
        net.sendMessage("disconnect:"+MainActivity.getNickName(),NetworkScanHost.players_port,host.getAddress());

    }

    @Override
    protected void onStop()
    {
        super.onStop();
        listener.cancel(true);
        net.sendMessage("disconnect:"+MainActivity.getNickName(),NetworkScanHost.players_port,host.getAddress());
    }




    class HostListener extends AsyncTask<Void, String, Void>
    {
        boolean end = false;
        void endActivity()
        {
            end = true;
        }

        NetworkManager.ListenerCallBack action = null;
        @Override
        protected Void doInBackground(Void... params)
        {

            action = net.listen(NetworkManager.servers_port, new NetworkManager.ListenerCallBack() {
                @Override
                public void listeningAction(String receivedMessage, InetAddress sender) {
                    Log.d("ClientActivity","listening action start");
                    String[] ps = new String[0];
                    if(receivedMessage.matches("players:((.|\n)*)")) {
                        ps = NetworkScanHost.parsePlayers(receivedMessage);
                        if (!compare(ps, players)) {
                            players = ps;
                            publishProgress();
                        }
                    }
                    else if(receivedMessage.matches("disconnect:(.*)") && host.getAddress().getHostAddress().equals(sender.getHostAddress())) {
                        endActivity();
                    }
                }
            });

            while(true)
            {
                if(isCancelled() || end)
                {
                    net.cancelActionInPort(NetworkManager.servers_port,action);
                    return null;
                }

            }

        }

        boolean compare(String[] p1,String[] p2)
        {
            if(p1.length != p2.length) return false;
            for(int i=0;i<p1.length;i++)
                if(!p1[i].equals(p2[i]))
                    return false;
            return true;
        }
        @Override
        protected void onProgressUpdate(String... progress)
        {
            ArrayAdapter adapter = new ArrayAdapter<String>(act,
                    R.layout.activity_listview, players);
            myList.setAdapter(adapter);
        }


        @Override
        protected void onPostExecute(Void result)
        {
            if(end) {
                Toast.makeText(act, "host ended!", Toast.LENGTH_SHORT).show();
                act.finish();
            }
            super.onPostExecute(result);
        }

    }

}
