package tw.edu.yzu.cse.arhideandseek;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class Room extends AppCompatActivity {
    ListView teamA = null;
    ListView teamB = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.room);
        teamA = (ListView) findViewById(R.id.teamA);
        teamB = (ListView) findViewById(R.id.teamB);
        LayoutInflater inflater = getLayoutInflater();
        teamA.addHeaderView((ViewGroup) inflater.inflate(R.layout.teama, teamA, false));
        teamB.addHeaderView((ViewGroup) inflater.inflate(R.layout.teamb, teamB, false));
        ((TextView) findViewById(R.id.stat)).setText("Room " + Game.roomID);
        initTeam();
        Game.client.handler = handler;
        ((Button) findViewById(R.id.exit)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Game.client.Close();
                Room.this.finish();
            }
        });
        Button start = (Button) findViewById(R.id.start);
        if (!Game.isHost) {
            start.setVisibility(View.GONE);
        } else {
            start.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Game.client.Send(Game.roomID + "START");
                }
            });
        }
    }

    private void initTeam() {
        if (!Game.teamA.isEmpty()) {
            teamA.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, Game.teamA.split(";")));
        } else {
            teamA.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, new String[]{"空"}));
        }
        if (!Game.teamB.isEmpty()) {
            teamB.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, Game.teamB.split(";")));
        } else {
            teamB.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, new String[]{"空"}));
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    initTeam();
                    break;
                case 2:
                    Game.client.handler = null;
                    Intent intent = new Intent();
                    intent.setClass(Room.this, Game.class);
                    startActivity(intent);
                    Room.this.finish();
                    break;
            }
        }
    };

}
