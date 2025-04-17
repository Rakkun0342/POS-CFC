package com.example.drinkcourt.conn;

import android.content.Context;
import android.os.StrictMode;
import android.util.Log;

import com.example.drinkcourt.utils.ConfigIP;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Connect {

    public Connection connection(Context context) {
        ConfigIP configIP = new ConfigIP(context);

        StrictMode.ThreadPolicy p = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(p);
        Connection con = null;
        String url = null;
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            url = "jdbc:jtds:sqlserver://" + configIP.getiP() + ";" + "databaseName=" + configIP.getDb() + ";" + "user=" + configIP.getUser() + ";" + "password=" + configIP.getPas() + ";";
            con = DriverManager.getConnection(url);
        } catch (SQLException a){
            Log.e("Error SQL:", a.getMessage());
        } catch (Exception e) {
            Log.e("Error :", e.getMessage());
        }
        return con;
    }
}
