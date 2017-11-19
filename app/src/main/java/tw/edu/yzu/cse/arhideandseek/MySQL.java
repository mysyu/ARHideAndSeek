package tw.edu.yzu.cse.arhideandseek;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MySQL {
    private static Connection con = null;
    private static Integer excuteResult = null;
    private static ResultSet selectResult = null;
    private static Exception ex = null;
    public static String IP = null;
    public static Context context = null;

    public static void Connect() throws Exception {
        ex = null;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!HasInternet()) throw new Exception("Can not connect to Internet");
                    if (con == null || !con.isValid(1)) {
                        Class.forName("com.mysql.jdbc.Driver");
                        con = DriverManager.getConnection("jdbc:mysql://mysyu.ddns.net:3306/arhideandseek", "arhideandseek", "arhideandseek");
                    }
                } catch (Exception e) {
                    ex = e;
                }
            }
        });
        t.start();
        try {
            t.join();
        } catch (Exception e) {
            ex = e;
        }
        if (ex != null) throw ex;
    }

    public static void Disconnect() {
        con = null;
    }

    public static ResultSet Select(final String sql, final Object[] parm) throws Exception {
        ex = null;
        selectResult = null;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement stmt = con.prepareStatement(sql);
                    for (int i = 1; i <= parm.length; i++) {
                        stmt.setObject(i, parm[i - 1]);
                    }
                    selectResult = stmt.executeQuery();
                } catch (Exception e) {
                    ex = e;
                }
            }
        });
        t.start();
        try {
            t.join();
        } catch (Exception e) {
            ex = e;
        }
        if (ex != null) throw ex;
        return selectResult;
    }

    public static Integer Excute(final String sql, final Object[] parm) throws Exception {
        ex = null;
        excuteResult = null;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement stmt = con.prepareStatement(sql);
                    for (int i = 1; i <= parm.length; i++) {
                        stmt.setObject(i, parm[i - 1]);
                    }
                    excuteResult = stmt.executeUpdate();
                } catch (Exception e) {
                    ex = e;
                }
            }
        });
        t.start();
        try {
            t.join();
        } catch (Exception e) {
            ex = e;
        }
        if (ex != null) throw ex;
        return excuteResult;
    }

    public static Boolean HasInternet() throws Exception {
        try {
            IP = null;
            boolean temp = true;
            boolean haveConnectedWifi = false;
            boolean haveConnectedMobile = false;
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnectedOrConnecting() && activeNetwork.isAvailable()) {
                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                envelope.dotNet = true;
                SoapObject request = new SoapObject("http://tempuri.org/", "IP");
                envelope.setOutputSoapObject(request);
                HttpTransportSE androidHttpTransport = new HttpTransportSE("http://mysyu.ddns.net/WebService.asmx");
                androidHttpTransport.call("http://tempuri.org/IP", envelope);
                IP = envelope.getResponse().toString();
            }
        } catch (Exception e) {
            ex = e;
        }
        if (ex != null) throw ex;
        return IP != null;
    }

}
