package tw.edu.yzu.cse.arhideandseek;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Room extends AppCompatActivity {
    ListView teamA = null;
    ListView teamB = null;

    TextView team_a_score = null;
    TextView team_b_score = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.room);
        teamA = (ListView) findViewById(R.id.teamA);
        teamB = (ListView) findViewById(R.id.teamB);
        LayoutInflater inflater = getLayoutInflater();
        teamA.addHeaderView(inflater.inflate(R.layout.teama, teamA, false));
        teamB.addHeaderView(inflater.inflate(R.layout.teamb, teamB, false));
        ((TextView) findViewById(R.id.stat)).setText("Room " + Game.roomID);
        team_a_score = (TextView) findViewById(R.id.team_a_score);
        team_b_score = (TextView) findViewById(R.id.team_b_score);

        if (Game.status == 0) {
            initTeam();
            Game.client.handler = handler;
            findViewById(R.id.exit).setOnClickListener(new View.OnClickListener() {
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
            team_a_score.setVisibility(View.GONE);
            team_b_score.setVisibility(View.GONE);

        } else {
            Button next = (Button) findViewById(R.id.start);
            next.setText("Stat");
            next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setClass(Room.this, Stat.class);
                    startActivity(intent);
                    Room.this.finish();
                }
            });

            findViewById(R.id.exit).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Game.client.Close();
                    Room.this.finish();
                }
            });
            team_a_score.setVisibility(View.VISIBLE);
            team_b_score.setVisibility(View.VISIBLE);

            setStat();

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

    private void setStat() {

        String[] player_score_a = null;
        String[] player_score_b = null;
        Integer score_a = 0;
        Integer score_b = 0;

        if (!Game.teamA.isEmpty()) {
            player_score_a = Game.teamA.split(";");
            for (int i = 0; i < Game.team_a_score.length; i++) {
                player_score_a[i] += ("\t\t\t找到 " + Game.team_a_score[i] + " 個");
                score_a += Game.team_a_score[i];
            }
            teamA.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, player_score_a));
        } else {
            teamA.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, new String[]{"空"}));
        }
        if (!Game.teamB.isEmpty()) {
            player_score_a = Game.teamB.split(";");
            for (int i = 0; i < Game.team_b_score.length; i++) {
                player_score_a[i] += ("\t\t\t找到 " + Game.team_b_score[i] + " 個");
                score_b += Game.team_b_score[i];
            }
            teamB.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, player_score_b));
        } else {
            teamB.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, new String[]{"空"}));
        }
        team_a_score.setText(score_a + "");
        team_b_score.setText(score_b + "");
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
                    Game.team_a_score = new Integer[Game.teamA.split(";").length];
                    for (int i = 0; i < Game.team_a_score.length; i++) {
                        Game.team_a_score[i] = 0;
                    }
                    Game.team_b_score = new Integer[Game.teamB.split(";").length];
                    for (int i = 0; i < Game.team_b_score.length; i++) {
                        Game.team_b_score[i] = 0;
                    }
                    Intent intent = new Intent();
                    intent.setClass(Room.this, Game.class);
                    startActivity(intent);
                    Room.this.finish();
                    break;
                case 7:
                    setStat();
                    if (Game.status == 3) {
                        int current = msg.getData().getInt("SEEK", -1);
                        String who = msg.getData().getString("WHO", "");
                        Log.e("game", "SEEK" + current + ":" + who);
                        Toast.makeText(Room.this, "SEEK" + current + ":" + who, Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

}
