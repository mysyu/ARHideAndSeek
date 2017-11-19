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

import java.sql.ResultSet;

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
                        Game.name += "-" + MySQL.IP;
                        Log.e("Name", Game.name);
                        ResultSet result = MySQL.Select("SELECT host FROM game WHERE ID=? LIMIT 1", new Object[]{Game.roomID});
                        if (!result.next())
                            throw new Exception("Can not find the room ID = " + Game.roomID);
                        Game.host = result.getString("host");
                        Game.isHost = false;
                        Game.client = new Client();
                        Game.client.handler = handler;
                        Game.client.Start();
                    }
                } catch (Exception e) {
                    Log.e("err", Log.getStackTraceString(e));
                    if (e.getMessage().equals("Can not find the room ID = " + Game.roomID))
                        Toast.makeText(GameFind.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    else
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
        Game.roomID = r;
        return true;
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Game.client.handler = null;
                    Intent intent = new Intent();
                    intent.setClass(GameFind.this, Room.class);
                    startActivity(intent);
                    GameFind.this.finish();
                    break;
                case 1:
                    Game.client.Send(Game.roomID + "PLAYER:" + Game.name);
                    break;
            }
        }
    };


}
