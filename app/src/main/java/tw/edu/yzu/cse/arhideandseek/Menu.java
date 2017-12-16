package tw.edu.yzu.cse.arhideandseek;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class Menu extends AppCompatActivity {
    private EditText name = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);
        MySQL.context = this;
        name = (EditText) findViewById(R.id.name);
        findViewById(R.id.host).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("menu", "host");
                if (validateName()) {
                    Intent intent = new Intent();
                    intent.setClass(Menu.this, GameCreate.class);
                    startActivity(intent);
                }
            }
        });
        findViewById(R.id.player_name).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("menu", "player");
                if (validateName()) {
                    Intent intent = new Intent();
                    intent.setClass(Menu.this, GameFind.class);
                    startActivity(intent);
                }
            }
        });

    }

    private Boolean validateName() {
        String n = name.getText().toString();
        if (!n.matches("^[a-zA-Z0-9]{1,10}$")) {
            name.setError(name.getHint());
            return false;
        }
        Game.name = n;
        return true;
    }

}
