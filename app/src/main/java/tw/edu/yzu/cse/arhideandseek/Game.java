package tw.edu.yzu.cse.arhideandseek;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class Game extends AppCompatActivity {
    public static String name = null;
    public static String roomID = null;
    public static String host = null;
    public static Boolean isHost = null;
    public static Integer time = null;
    public static Boolean useCardBoard = null;
    public static Integer treasure = null;
    public static String teamA = null;
    public static String teamB = null;
    public static Client client = null;
    public static Integer status = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_create);
    }

}
