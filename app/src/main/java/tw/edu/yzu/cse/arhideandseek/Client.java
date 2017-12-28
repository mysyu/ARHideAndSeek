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
        super(URI.create("ws://mysyu.ddns.net:666/"), new Draft_6455());
    }

    public void Start() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Static.client.connect();
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
                Static.client.send(s);
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
                if (Static.status == 0) {
                    if (Static.isHost) {
                        Static.client.send(Static.roomID + "EXIT");
                    } else {
                        Static.client.send(Static.roomID + "LEAVE:" + Static.name);
                    }
                }
                Static.client.close();
                Static.client = null;
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
        if (s.startsWith(Static.roomID)) {
            s = s.replaceFirst(Static.roomID, "");
            Log.e("client", s);
            if (s.startsWith("ROOM:")) {
                s = s.replaceFirst("ROOM:", "");
                Log.e("client", "Time:" + Static.time + ",CardBoard:" + Static.useCardBoard + ",Treasure:" + Static.treasure + ",TeamA:" + Static.teamA + ",TeamB:" + Static.teamB + ",Status:" + Static.status);
                String[] ss = s.split(",");
                Static.time = Integer.parseInt(ss[0].replaceFirst("Time:", ""));
                Static.useCardBoard = Boolean.parseBoolean(ss[1].replaceFirst("CardBoard:", ""));
                Static.treasure = Integer.parseInt(ss[2].replaceFirst("Treasure:", ""));
                Static.teamA = ss[3].replaceFirst("TeamA:", "");
                Static.teamB = ss[4].replaceFirst("TeamB:", "");
                Static.status = Integer.parseInt(ss[5].replaceFirst("Status:", ""));
                handler.sendEmptyMessage(0);
                Log.e("client", s);
            } else if (s.equals("START")) {
                Static.status = 1;
                handler.sendEmptyMessage(2);
            } else if (s.startsWith("PLAY:")) {
                s = s.replaceFirst("PLAY:", "");
                if (s.equals("Ready")) {
                    Static.status = 2;
                } else if (s.equals("Start")) {
                    Static.status = 3;
                }
                Bundle bundle = new Bundle();
                bundle.putString("PLAY", s);
                Message message = new Message();
                message.what = 6;
                message.setData(bundle);
                handler.sendMessage(message);
                Log.e("client", s);
            } else if (s.startsWith("HIDE")) {
                if (!Static.isHost) {
                    String[] ss = s.split(";");
                    final int current = Integer.parseInt(ss[1]);
                    Static.hide[current] = ss[2];
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ResultSet result = MySQL.Select("SELECT capture FROM capture WHERE ID=? AND type='HIDE' AND position=?", new Object[]{Static.roomID, current});
                                result.next();
                                byte[] bytes = result.getBytes("capture");
                                Static.Img_hide[current] = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                if (current + 1 == Static.hide.length) {
                                    for (int i = 0; i < Static.hide.length - 1; i++) {
                                        if (Static.Img_hide[i] == null) {
                                            i--;
                                        }
                                    }
                                    Static.client.Send(Static.roomID + "PLAY");
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
                Static.seek[current] = ss[3];
                if (Static.teamA.contains(ss[3])) {
                    Static.team_a_score[Arrays.asList(Static.teamA.split(";")).indexOf(ss[3])]++;
                    Static.score_a++;
                } else if (Static.teamB.contains(ss[3])) {
                    Static.team_b_score[Arrays.asList(Static.teamB.split(";")).indexOf(ss[3])]++;
                    Static.score_b++;
                }
                Bundle bundle = new Bundle();
                bundle.putInt("SEEK", current);
                bundle.putString("WHO", ss[3]);
                Message message = new Message();
                message.what = 7;
                message.setData(bundle);
                handler.sendMessage(message);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ResultSet result = MySQL.Select("SELECT capture FROM capture WHERE ID=? AND type='SEEK' AND position=?", new Object[]{Static.roomID, current});
                            result.next();
                            byte[] bytes = result.getBytes("capture");
                            Static.Img_seek[current] = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            handler.sendEmptyMessage(10);
                        } catch (Exception e) {
                            Log.e("client", Log.getStackTraceString(e));
                        }
                    }
                }).start();
            } else if (s.equals("FINISH")) {
                Static.status = 4;
                handler.sendEmptyMessage(9);
            } else if (s.equals("EXIT")) {
                Static.client.Close();
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
