package tw.edu.yzu.cse.arhideandseek;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class Stat extends AppCompatActivity {
    private Button change = null;
    private Integer current = null;
    private ImageView Img_hide = null;
    private ImageView Img_seek = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stat);
      //  change = (Button) findViewById(R.id.change);
        /* change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (change.getText().toString().equals("Hide")) {
                    change.setText("Seek");
                } else if (change.getText().toString().equals("Seek")) {
                    change.setText("Hide");
                }
                setImage();
            }
        });*/
        findViewById(R.id.last).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                current = (current - 1 + Game.hide.length) % Game.hide.length;
                setImage();
            }
        });
        findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                current = (current + 1 + Game.hide.length) % Game.hide.length;
                setImage();
            }
        });
        findViewById(R.id.detail).setOnClickListener(new View.OnClickListener() {
            @Override
               public void onClick(View v) {
                Stat.this.finish();
               }
        });
        Img_hide = (ImageView) findViewById(R.id.imageView1);
        Img_seek = (ImageView) findViewById(R.id.imageView1);
        current = 0;
        setImage();
        Game.client.handler = handler;
    }

    private void setImage() {
        Img_hide.setImageBitmap(Game.hide[current]);
        Img_seek.setImageBitmap(Game.seek[current]);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 4:
                    Log.e("stat", msg.getData().getString("PLAY", ""));
                    break;
            }
        }
    };

}
