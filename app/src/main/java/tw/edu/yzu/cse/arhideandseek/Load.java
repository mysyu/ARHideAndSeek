package tw.edu.yzu.cse.arhideandseek;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

public class Load extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.load);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 0);
            } else {
                handler.sendEmptyMessage(-1);
            }
        } else {
            handler.sendEmptyMessage(0);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0 && requestCode == 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            handler.sendEmptyMessage(-1);
        } else {
            handler.sendEmptyMessage(0);
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(3000);
                                handler.sendEmptyMessage(1);
                            } catch (InterruptedException e) {
                                Log.e("load", Log.getStackTraceString(e));
                            }
                        }
                    }).start();
                    break;
                case 1:
                    Intent intent = new Intent();
                    intent.setClass(Load.this, Menu.class);
                    startActivity(intent);
                    Load.this.finish();
                    break;
                case -1:
                    Toast.makeText(Load.this, "You do not have camera permission", Toast.LENGTH_SHORT).show();
                    Load.this.finish();
                    break;
            }
        }
    };

}
