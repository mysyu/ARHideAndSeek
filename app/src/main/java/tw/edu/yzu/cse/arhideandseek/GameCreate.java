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
                    if (Game.client == null) {
                        MySQL.Connect();
                        Game.name += "-" + MySQL.IP;
                        Log.e("Name", Game.name);
                        MySQL.Excute("DELETE FROM game WHERE host like ?", new Object[]{"%" + MySQL.IP + "%"});
                        if (MySQL.Excute("INSERT INTO game VALUES(GenerateRoomID(),?)", new Object[]{Game.name}) == 1) {
                            ResultSet result = MySQL.Select("SELECT ID FROM game WHERE host=? LIMIT 1", new Object[]{Game.name});
                            result.next();
                            Game.roomID = result.getString("ID");
                            Game.host = Game.name;
                            Game.isHost = true;
                            Game.time = (((Spinner) findViewById(R.id.time)).getSelectedItemPosition() + 1) * 5 * 60;
                            Game.useCardBoard = (((Spinner) findViewById(R.id.mode)).getSelectedItemPosition()) == 1;
                            Game.treasure = (((Spinner) findViewById(R.id.time)).getSelectedItemPosition() + 1) * 5;
                            Game.teamA = "";
                            Game.teamB = "";
                            Game.status = 0;
                            Log.e("start", "Name: " + Game.name + ",RoomID: " + Game.roomID + ",Time: " + Game.time + ",CardBoard: " + Game.useCardBoard + ",Treasure: " + Game.treasure);
                            Game.client = new Client();
                            Game.client.handler = handler;
                            Game.client.Start();
                        }
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
                    Game.client.handler = null;
                    Intent intent = new Intent();
                    intent.setClass(GameCreate.this, Room.class);
                    startActivity(intent);
                    GameCreate.this.finish();
                    break;
                case 1:
                    Game.client.Send(Game.roomID + "HOST:" + Game.name + ",Time:" + Game.time + ",CardBoard:" + Game.useCardBoard + ",Treasure:" + Game.treasure);
                    break;
            }
        }
    };

}
