package marcogino.bluemotobase;

import android.bluetooth.BluetoothDevice;

//import marcogino.bluemoto.DbManager;

/**
 * Created by Marco_76 on 08/08/2016.
 */
public class Constants {

    public static final int ERROR_CODE = 999;
    public static  double SPEED_MIN_TO_RECORD = 3d;
    public static  double SPEED_MIN = 0;
    public static  double SPEED_MAX = 0;
    public static final int SET_SPEED_MIN = 5;
    public static final int SET_SPEED_MAX = 50;
    public static final int TIMEOUT_MIN = 10;
    public static final int TIMEOUT_MAX = 250;
    public static boolean CONNECTED_TO_DEVICE = false;
    public static final int PASSWORD_ACTIVITY = 10;  // The request code
    public static BluetoothDevice BT_DEVICE = null;
    public static String BT_DEVICE_NAME_SELECTED="";
    public static Double SPEED_FACTOR = 1.0d;
    public static Double SPEED = 0.0d;
    public static String STARTFOREGROUND_ACTION = "it.marcogino.bluemoto.MotoConnectionService.action.startforeground";
    public static String STOPFOREGROUND_ACTION = "it.marcogino.bluemoto.MotoConnectionService.action.stopforeground";
    //public static DbManager DBMANAGER=null;
    public static Boolean LOGGING_ENABLED = false;

}
