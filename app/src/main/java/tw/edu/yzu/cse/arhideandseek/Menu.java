package tw.edu.yzu.cse.arhideandseek;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class Menu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);
        ((Button) findViewById(R.id.host)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("menu", "host");
                Intent intent = new Intent();
                intent.setClass(Menu.this, GameCreate.class);
                startActivity(intent);
            }
        });
        ((Button) findViewById(R.id.player)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("menu", "player");
                Intent intent = new Intent();
                intent.setClass(Menu.this, GameFind.class);
                startActivity(intent);
            }
        });

    }
}
