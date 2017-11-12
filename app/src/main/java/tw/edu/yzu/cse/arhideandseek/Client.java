package tw.edu.yzu.cse.arhideandseek;

import android.os.Handler;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

/**
 * Created by mysyu on 2017/11/11.
 */

public class Client extends WebSocketClient {

    public Handler handler = null;

    public Client() {
        super(URI.create("ws://1.34.30.96:666/"), new Draft_6455());
    }

    public void Start() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Game.client.connect();
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Log.e("client", Log.getStackTraceString(e));
        }
    }

    public void Send(final String s) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Game.client.send(s);
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Log.e("client", Log.getStackTraceString(e));
        }
    }

    public void Close() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (Game.isHost) {
                    Game.client.Send(Game.roomID + "EXIT");
                } else {
                    Game.client.Send(Game.roomID + "LEAVE:" + Game.name);
                }
                Game.client.close();
                Game.client = null;
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Log.e("client", Log.getStackTraceString(e));
        }
    }


    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        Log.e("client", "connect");
        handler.sendEmptyMessage(1);
    }

    @Override
    public void onMessage(String s) {
        if (s.startsWith(Game.roomID)) {
            s = s.replaceFirst(Game.roomID, "");
            Log.e("client", s);
            if (s.startsWith("ROOM:")) {
                s = s.replaceFirst("ROOM:", "");
                Log.e("client", "Time:" + Game.time + ",CardBoard:" + Game.useCardBoard + ",Treasure:" + Game.treasure + ",TeamA:" + Game.teamA + ",TeamB:" + Game.teamB + ",Status:" + Game.status);
                String[] ss = s.split(",");
                Game.time = Integer.parseInt(ss[0].replaceFirst("Time:", ""));
                Game.useCardBoard = Boolean.parseBoolean(ss[1].replaceFirst("CardBoard:", ""));
                Game.treasure = Integer.parseInt(ss[2].replaceFirst("Treasure:", ""));
                Game.teamA = ss[3].replaceFirst("TeamA:", "");
                Game.teamB = ss[4].replaceFirst("TeamB:", "");
                Game.status = Integer.parseInt(ss[5].replaceFirst("Status:", ""));
                handler.sendEmptyMessage(0);
                Log.e("client", s);
            } else if (s.equals("EXIT")) {
                Game.client.Close();
                handler.sendEmptyMessage(-1);
            }
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        Log.e("client", s);
    }

    @Override
    public void onError(Exception e) {
        Log.e("client", Log.getStackTraceString(e));
    }
}