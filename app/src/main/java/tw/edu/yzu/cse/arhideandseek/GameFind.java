package tw.edu.yzu.cse.arhideandseek;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class GameFind extends AppCompatActivity {
    private EditText roomID = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_find);
        roomID = (EditText) findViewById(R.id.roomID);
        findViewById(R.id.join).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("find", "player");
                try {
                    if (validateRoomID()) {
                        MySQL.Connect();
                        if (!Static.name.contains("-")) {
                            Static.name += "-" + MySQL.IP;
                        }
                        Static.isHost = false;
                        Static.client = new Client();
                        Static.client.handler = handler;
                        Static.client.Start();
                    }
                } catch (Exception e) {
                    Log.e("err", Log.getStackTraceString(e));
                    Toast.makeText(GameFind.this, "Can not connect to Server. Please check the network and try again later.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private Boolean validateRoomID() {
        String r = roomID.getText().toString();
        if (!r.matches("^[0-9]{10}$")) {
            roomID.setError(roomID.getHint());
            return false;
        }
        Static.roomID = r;
        return true;
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    if (Static.status == 0) {
                        if (Static.teamA.contains(Static.name) || Static.teamB.contains(Static.name)) {
                            Static.client.handler = null;
                            Intent intent = new Intent();
                            intent.setClass(GameFind.this, Room.class);
                            startActivity(intent);
                            GameFind.this.finish();
                            break;
                        } else {
                            Toast.makeText(GameFind.this, "You can not enter this room.", Toast.LENGTH_SHORT).show();
                            Static.status = -1;
                            Static.client.Close();
                        }
                    } else {
                        Toast.makeText(GameFind.this, "The Room has started the Game. Please wait for next Game.", Toast.LENGTH_SHORT).show();
                        Static.client.Close();
                    }
                    break;
                case 1:
                    Static.client.Send(Static.roomID + "PLAYER:" + Static.name);
                    break;
                case -3:
                    Toast.makeText(GameFind.this, "Can not find the room ID = " + Static.roomID, Toast.LENGTH_SHORT).show();
                    Static.status = -1;
                    Static.client.Close();
                    break;

            }
        }
    };


}
