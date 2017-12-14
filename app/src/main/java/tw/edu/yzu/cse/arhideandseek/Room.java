package tw.edu.yzu.cse.arhideandseek;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class Room extends AppCompatActivity {
    ListView teamA = null;
    ListView teamB = null;

    Integer now_score_a = 0;
    Integer now_score_b = 0;

    String[] spilit_a = new String[]{};
    String[] spilit_b = new String[]{};

    String[] player_score_a  = new String[]{};
    String[] player_score_b  = new String[]{};

    String[] show = new String[]{};

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
        }else{
            // 加入個人比分
            initTeam_score();
            // Next 按鈕 (to stat page)
            Button next = (Button) findViewById(R.id.start);
            next.setText("NEXT");

            next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setClass(Room.this, Stat.class);
                    startActivity(intent);
                }
            });

            // 離開按鈕
            findViewById(R.id.exit).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Game.client.Close();
                    Room.this.finish();
                }
            });

            // 加入團隊比分
            TextView team_a_score = (TextView) findViewById(R.id.team_a_score);
            team_a_score.setVisibility(View.VISIBLE);
            for (int i = 0; i < Game.team_a_score.length; i++)
                now_score_a += Game.team_a_score[i];
            team_a_score.setText(now_score_a + "");

            TextView team_b_score = (TextView) findViewById(R.id.team_b_score);
            team_b_score.setVisibility(View.VISIBLE);
            for (int i = 0; i < Game.team_b_score.length; i++)
                now_score_b += Game.team_b_score[i];
            team_b_score.setText(now_score_b + "");

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

    private void initTeam_score() {
        if (!Game.teamA.isEmpty()) {

            spilit_a = Game.teamA.split(";");
            for(int i = 0; i < Game.team_a_score.length; i++)
                player_score_a[i] = Integer.toString(Game.team_a_score[i]);
            // 顯示 玩家 與 分數
            for(int i = 0; i < spilit_a.length; i++)
                show[i] = spilit_a[i] + "\t\t\t找到 " + player_score_a[i] + " 個";

            teamA.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, show));
        } else {
            teamA.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, new String[]{"空"}));
        }
        if (!Game.teamB.isEmpty()) {

            spilit_b = Game.teamB.split(";");
            for(int i = 0; i < Game.team_b_score.length; i++)
                player_score_b[i] = Integer.toString(Game.team_b_score[i]);
            // 顯示 玩家 與 分數
            for(int i = 0; i < spilit_b.length; i++)
                show[i] = spilit_b[i] + "\t\t\t找到 " + player_score_b[i] + " 個";

            teamB.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, show));
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
