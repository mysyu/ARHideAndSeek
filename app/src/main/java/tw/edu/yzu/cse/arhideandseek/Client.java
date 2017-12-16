package tw.edu.yzu.cse.arhideandseek;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.sql.ResultSet;
import java.util.Arrays;

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
        while (handler == null) ;
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
            } else if (s.equals("START")) {
                Game.status = 1;
                handler.sendEmptyMessage(2);
            } else if (s.startsWith("PLAY:")) {
                s = s.replaceFirst("PLAY:", "");
                if (s.equals("Ready")) {
                    Game.status = 2;
                } else if (s.equals("Start")) {
                    Game.status = 3;
                }
                Bundle bundle = new Bundle();
                bundle.putString("PLAY", s);
                Message message = new Message();
                message.what = 6;
                message.setData(bundle);
                handler.sendMessage(message);
                Log.e("client", s);
            } else if (s.startsWith("HIDE")) {
                if (!Game.isHost) {
                    String[] ss = s.split(";");
                    final int current = Integer.parseInt(ss[1]);
                    Game.hide[current] = ss[2];
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ResultSet result = MySQL.Select("SELECT capture FROM capture WHERE ID=? AND type='HIDE' AND position=?", new Object[]{Game.roomID, current});
                                result.next();
                                byte[] bytes = result.getBytes("capture");
                                Game.Img_hide[current] = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                if (current + 1 == Game.hide.length) {
                                    for (int i = 0; i < Game.hide.length - 1; i++) {
                                        if (Game.Img_hide[i] == null) {
                                            i--;
                                        }
                                    }
                                    Game.client.Send(Game.roomID + "PLAY");
                                }
                            } catch (Exception e) {
                                Log.e("client", Log.getStackTraceString(e));
                            }
                        }
                    }).start();
                }
            } else if (s.startsWith("SEEK")) {
                final String[] ss = s.split(";");
                final int current = Integer.parseInt(ss[1]);
                Game.seek[current] = ss[3];
                if (Game.teamA.contains(ss[3])) {
                    Game.team_a_score[Arrays.asList(Game.teamA.split(";")).indexOf(ss[3])]++;
                } else if (Game.teamB.contains(ss[3])) {
                    Game.team_b_score[Arrays.asList(Game.teamB.split(";")).indexOf(ss[3])]++;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ResultSet result = MySQL.Select("SELECT capture FROM capture WHERE ID=? AND type='SEEK' AND position=?", new Object[]{Game.roomID, current});
                            result.next();
                            byte[] bytes = result.getBytes("capture");
                            Game.Img_seek[current] = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            Bundle bundle = new Bundle();
                            bundle.putInt("SEEK", current);
                            bundle.putString("WHO", ss[3]);
                            Message message = new Message();
                            message.what = 7;
                            message.setData(bundle);
                            handler.sendMessage(message);
                        } catch (Exception e) {
                            Log.e("client", Log.getStackTraceString(e));
                        }
                    }
                }).start();
            } else if (s.equals("FINISH")) {
                Game.status = 4;
                handler.sendEmptyMessage(9);
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
