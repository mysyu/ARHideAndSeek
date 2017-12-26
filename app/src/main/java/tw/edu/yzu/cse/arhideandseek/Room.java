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
        ((TextView) findViewById(R.id.stat)).setText("Room " + Static.roomID);
        team_a_score = (TextView) findViewById(R.id.team_a_score);
        team_b_score = (TextView) findViewById(R.id.team_b_score);

        if (Static.status == 0) {
            initTeam();
            findViewById(R.id.exit).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Static.client.Close();
                    Room.this.finish();
                }
            });
            Button start = (Button) findViewById(R.id.start);
            if (!Static.isHost) {
                start.setVisibility(View.GONE);
            } else {
                start.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (Static.teamA.isEmpty() && Static.teamB.isEmpty()) {
                            Toast.makeText(Room.this, "You can not start the game with no player", Toast.LENGTH_SHORT).show();
                        } else {
                            Static.client.Send(Static.roomID + "START");
                        }
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
                    Static.client.Close();
                    Room.this.finish();
                }
            });
            team_a_score.setVisibility(View.VISIBLE);
            team_b_score.setVisibility(View.VISIBLE);

            setStat();

        }
        Static.client.handler = handler;
    }

    private void initTeam() {
        if (!Static.teamA.isEmpty()) {
            teamA.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, Static.teamA.split(";")));
        } else {
            teamA.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, new String[]{"空"}));
        }
        if (!Static.teamB.isEmpty()) {
            teamB.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, Static.teamB.split(";")));
        } else {
            teamB.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, new String[]{"空"}));
        }
    }

    private void setStat() {

        String[] player_score_a = null;
        String[] player_score_b = null;

        if (!Static.teamA.isEmpty()) {
            player_score_a = Static.teamA.split(";");
            for (int i = 0; i < Static.team_a_score.length; i++) {
                player_score_a[i] += ("\t\t\t找到 " + Static.team_a_score[i] + " 個");
            }
            teamA.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, player_score_a));
        } else {
            teamA.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, new String[]{"空"}));
        }
        if (!Static.teamB.isEmpty()) {
            player_score_a = Static.teamB.split(";");
            for (int i = 0; i < Static.team_b_score.length; i++) {
                player_score_a[i] += ("\t\t\t找到 " + Static.team_b_score[i] + " 個");
            }
            teamB.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, player_score_b));
        } else {
            teamB.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, new String[]{"空"}));
        }
        team_a_score.setText(Static.score_a + "");
        team_b_score.setText(Static.score_b + "");
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    initTeam();
                    break;
                case 2:
                    Static.client.handler = null;
                    Static.team_a_score = new Integer[Static.teamA.split(";").length];
                    for (int i = 0; i < Static.team_a_score.length; i++) {
                        Static.team_a_score[i] = 0;
                    }
                    Static.score_a = 0;
                    Static.team_b_score = new Integer[Static.teamB.split(";").length];
                    for (int i = 0; i < Static.team_b_score.length; i++) {
                        Static.team_b_score[i] = 0;
                    }
                    Static.score_b = 0;
                    Intent intent = new Intent();
                    intent.setClass(Room.this, Game.class);
                    startActivity(intent);
                    Room.this.finish();
                    break;
                case 7:
                    int current = msg.getData().getInt("SEEK", -1);
                    String who = msg.getData().getString("WHO", "");
                    Log.e("game", "SEEK" + current + ":" + who);
                    Toast.makeText(Room.this, "SEEK" + current + ":" + who, Toast.LENGTH_SHORT).show();
                    setStat();
                    break;
            }
        }
    };

}
