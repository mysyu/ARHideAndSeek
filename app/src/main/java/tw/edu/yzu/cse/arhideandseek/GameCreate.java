package tw.edu.yzu.cse.arhideandseek;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Spinner;
import android.widget.Toast;

import java.sql.ResultSet;

public class GameCreate extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_create);
        findViewById(R.id.create).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("create", "host");
                try {
                    MySQL.Connect();
                    if (!Static.name.contains("-")) {
                        Static.name += "-" + MySQL.IP;
                    }
                    Log.e("Name", Static.name);
                    MySQL.Excute("DELETE FROM capture WHERE ID IN ( SELECT ID FROM game WHERE host like ?)", new Object[]{"%" + MySQL.IP + "%"});
                    MySQL.Excute("DELETE FROM game WHERE host like ?", new Object[]{"%" + MySQL.IP + "%"});
                    if (MySQL.Excute("INSERT INTO game VALUES(GenerateRoomID(),?)", new Object[]{Static.name}) == 1) {
                        ResultSet result = MySQL.Select("SELECT ID FROM game WHERE host=? LIMIT 1", new Object[]{Static.name});
                        result.next();
                        Static.roomID = result.getString("ID");
                        Static.host = Static.name;
                        Static.isHost = true;
                        Static.time = (((Spinner) findViewById(R.id.time)).getSelectedItemPosition() + 1) * 5 * 60;
                        Static.useCardBoard = (((Spinner) findViewById(R.id.mode)).getSelectedItemPosition()) == 1;
                        Static.treasure = Integer.parseInt(((Spinner) findViewById(R.id.treasure)).getSelectedItem().toString().replace("個", ""));
                        Static.teamA = "";
                        Static.teamB = "";
                        Static.status = 0;
                        Log.e("start", "Name: " + Static.name + ",RoomID: " + Static.roomID + ",Time: " + Static.time + ",CardBoard: " + Static.useCardBoard + ",Treasure: " + Static.treasure);
                        Static.client = new Client();
                        Static.client.handler = handler;
                        Static.client.Start();
                    }
                } catch (Exception e) {
                    Log.e("err", Log.getStackTraceString(e));
                    if (e.getMessage().equals("Server is busy. Please try again later."))
                        Toast.makeText(GameCreate.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(GameCreate.this, "Can not connect to Server. Please check the network and try again later.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Static.client.handler = null;
                    Intent intent = new Intent();
                    intent.setClass(GameCreate.this, Room.class);
                    startActivity(intent);
                    GameCreate.this.finish();
                    break;
                case 1:
                    Static.client.Send(Static.roomID + "HOST:" + Static.name + ",Time:" + Static.time + ",CardBoard:" + Static.useCardBoard + ",Treasure:" + Static.treasure);
                    break;
            }
        }
    };

}
