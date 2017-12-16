package tw.edu.yzu.cse.arhideandseek;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class Stat extends AppCompatActivity {
    private Button change = null;
    private Integer current = null;
    private ImageView Img_hide = null;
    private ImageView Img_seek = null;
    private TextView stat = null;
    private TextView position = null;
    private TextView found = null;
    private TextView player = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stat);
        findViewById(R.id.last).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                current = (current - 1 + Game.hide.length) % Game.hide.length;
                setStat();
            }
        });
        findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                current = (current + 1 + Game.hide.length) % Game.hide.length;
                setStat();
            }
        });
        findViewById(R.id.detail).setOnClickListener(new View.OnClickListener() {
            @Override
               public void onClick(View v) {
                Game.client.handler = null;
                Intent intent = new Intent();
                intent.setClass(Stat.this, Room.class);
                startActivity(intent);
                Stat.this.finish();
               }
        });
        Img_hide = (ImageView) findViewById(R.id.hide);
        Img_seek = (ImageView) findViewById(R.id.seek);
        stat = (TextView) findViewById(R.id.stat);
        position = (TextView) findViewById(R.id.position);
        found = (TextView) findViewById(R.id.found);
        player = (TextView) findViewById(R.id.player_name);

        current = 0;
        setStat();
        Game.client.handler = handler;
    }

    private void setStat() {
        Integer score_a = 0;
        Integer score_b = 0;

        if (!Game.teamA.isEmpty()) {
            for (int i = 0; i < Game.team_a_score.length; i++) {
                score_a += Game.team_a_score[i];
            }
        }
        if (!Game.teamB.isEmpty()) {
            for (int i = 0; i < Game.team_b_score.length; i++) {
                score_b += Game.team_b_score[i];
            }
        }
        stat.setText(String.format("TeamA  %02d  :  %02d   TeamB", score_a, score_b));
        position.setText("Position" + current);
        if (Game.seek[current] == null) {
            player.setText("");
            found.setText("Not Found");
        } else {
            player.setText("Player: " + Game.seek[current]);
            found.setText("Found");
        }
        Img_hide.setImageBitmap(Game.Img_hide[current]);
        Img_seek.setImageBitmap(Game.Img_seek[current]);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 4:
                    Log.e("stat", msg.getData().getString("PLAY", ""));
                    break;
                case 7:
                    setStat();
                    if (Game.status == 3) {
                        int current = msg.getData().getInt("SEEK", -1);
                        String who = msg.getData().getString("WHO", "");
                        Log.e("game", "SEEK" + current + ":" + who);
                        Toast.makeText(Stat.this, "SEEK" + current + ":" + who, Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

}
