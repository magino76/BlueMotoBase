package marcogino.bluemotobase;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Created by Marco_76 on 07/08/2016.
 */
public class MotoConnectionService extends Service {
    private NotificationManager nm;
    private NotificationManager nm1;
    private Timer timer = new Timer();
    private int counter = TIMER;
    private static boolean isRunning = false;
   // private Boolean tick = false;
    private Boolean passwordRichiesta = false;
    ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients.
    //int mValue = 0; // Holds last value set by a client.
    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_UNREGISTER_CLIENT = 2;
    public  static final int MSG_SET_INT_VALUE = 3;
    public static final int MSG_SET_STRING_VALUE = 4;
    public static final int MSG_SET_PSW_VALUE = 5;
    public static final int MSG_SET_DATA_VALUE = 6;
    static final int TIMER = 5;
    static final int REFRESH_TIME_UPDATE_INFO_SPEED = 200; //in msecondi
    private long pastTimeSeconds;

    static final long refreshTime = 1000L; // tempo di refresh di esecuzione routine di recezione dati buffer BT
    //static String connectionStateReceived ="";
    String connectionState="";
    final Messenger mMessenger = new Messenger(new IncomingHandler()); // Target we publish for clients to send messages to IncomingHandler.
    private BluetoothSocket bTSocket;
    private BluetoothDevice bTDevice;
    //BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Boolean eseguiBeepConnessioneRipristinata = true;
    Boolean eseguiBeepConnessionePersa = true;
    Boolean passwordOk = false;
    private String receivedData = "";
    Gson gson;
    String json;
    SharedPreferences prefs;
    byte[] readBuffer;
    int readBufferPosition;
    Boolean stopWorker;


    UUID uuid;
    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //Device found
                connectionState = BluetoothDevice.ACTION_FOUND;
            }
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Device is now connected
                connectionState = BluetoothDevice.ACTION_ACL_CONNECTED;
                stopWorker = false;
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //Done searching
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                //Device is about to disconnect
                connectionState = BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED;
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //Device has disconnected
                connectionState = BluetoothDevice.ACTION_ACL_DISCONNECTED;
                stopWorker = true;
            }
        }
    };


    @Override
    public IBinder onBind(Intent intent) {

        // gson = new Gson();
        // json = prefs.getString("BtDevice", "Device non trovato");
        // mmDevice = gson.fromJson(json, BluetoothDevice.class);

        return mMessenger.getBinder();
    }


    class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        String string ="";
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);

                    break;
                case MSG_SET_INT_VALUE:
                    //incrementby = msg.arg1;
                    break;
                case MSG_SET_DATA_VALUE:
                    string = msg.getData().getString("str1");
                    string = string.replaceFirst("^0+(?!$)", "");
                    try {
                        sendData(string+"\n");      //spedisco il dato per 3 volte consecutive


                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case MSG_SET_PSW_VALUE: // eseguito quando devo immettere la password

                    string = msg.getData().getString("str1");
                    try {
                        sendData("PSW" + string + "\n");

                        nuovoTimer();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // startTimer();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void nuovoTimer() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                onTimerTick();
            }
        }, 0, refreshTime);// ogni 1 secondi

    }

    private void sendMessageToUI(int intvaluetosend, String text) {
        for (int i = mClients.size() - 1; i >= 0; i--) {
            try {
                // Send data as an Integer
                mClients.get(i).send(Message.obtain(null, MSG_SET_STRING_VALUE, intvaluetosend, 0));

                //Send data as a String
                Bundle b = new Bundle();
                b.putString("str1", text);
                Message msg = Message.obtain(null, MSG_SET_STRING_VALUE);
                msg.setData(b);
                mClients.get(i).send(msg);

            } catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("MotoConnectionService", "Received start id " + startId + ": " + intent);
        if(Constants.STARTFOREGROUND_ACTION.equals(intent.getAction()))
        {
            CharSequence text = getText(R.string.service_started);
            // Set the icon, scrolling text and timestamp
            //Notification notification = new Notification(R.drawable.icon, text, System.currentTimeMillis());
            // The PendingIntent to launch our activity if the user selects this notification
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
            // Set the info for the views that show in the notification panel.

            Notification notification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_notifica)
                    .setContentText(text)
                    .setAutoCancel(true)
                    .setContentIntent(contentIntent)
                    .setContentTitle("BlueMoto").build();
            notification.flags = Notification.FLAG_ONGOING_EVENT; // RENDE LA NOTIFICA NON CANCELLABILE DALL'UTENTE.

            startForeground(startId, notification );
        }


        return START_STICKY; // run until explicitly stopped.
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        //IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, filter1);
        //this.registerReceiver(mReceiver, filter2);
        this.registerReceiver(mReceiver, filter3);

       // showNotification();
        isRunning = true;
        prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        //mac = prefs.getString("BtDevice", "No Preferences!");
        gson = new Gson();
        json = prefs.getString("BtDevice", "Device non trovato");
        try {
            mmDevice = gson.fromJson(json, BluetoothDevice.class);
        }
        catch (Exception e)
        {
            Toast.makeText(this, "Selezionare un dispositivo Bluetooth!!",Toast.LENGTH_LONG).show();

           // sendMessageToUI(MSG_SET_STRING_VALUE, "StopService");

        }
        uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID

       //prima di connettere verifico che sia abilitato il blurtooth sul cell

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                // Bluetooth is not enable :)
                Toast.makeText(this, "Abilitare il bluetooth!!!", Toast.LENGTH_LONG).show();

            }
            else
            {
                try {
                    connect(mmDevice, uuid);
                }
                catch (Exception IOe)
                {
                    sendMessageToUI(MSG_SET_STRING_VALUE, "Selezionre BT device...");
                }

                inizializzaDB();
                nuovoTimer();

            }
        }



    }


    private void inizializzaDB()
    {
        //Constants.DBMANAGER=new DbManager(this);
        //Cursor cursor=Constants.DBMANAGER.getSpeeds(); // ci restituisce un cursore con tutti i dati nella tabella
        // viene quindi assegnato il cursore al CursorAdapter pe creare l'elenco di View


    }


    public boolean connect(BluetoothDevice bTDevice, UUID mUUID) {
        this.bTSocket = null;
        this.bTDevice = bTDevice;
        //myLabel.setText("Bluetooth Opened");
        try {
            this.bTSocket = this.bTDevice.createRfcommSocketToServiceRecord(mUUID);

        } catch (IOException e) {
           // Log.d("CONNECTBT", "Could not create RFCOMM socket:" + e.toString());
            return false;
        }
        try {
            this.bTSocket.connect();
            mmOutputStream = this.bTSocket.getOutputStream();
            mmInputStream = this.bTSocket.getInputStream();
        } catch (IOException e) {
            //Log.d("CONNECTBT", "Could not connect: " + e.toString());

            try {
                this.bTSocket.close();
            } catch (IOException close) {
               // Log.d("CONNECTBT", "Could not close connection:" + e.toString());
                return false;
            }
        }


        return true;
    }

    private void resetConnection() {
        if (mmInputStream != null) {
            try {
                mmInputStream.close();
            } catch (Exception e) {
            }
            mmInputStream = null;
        }

        if (mmOutputStream != null) {
            try {
                mmOutputStream.close();
            } catch (Exception e) {
            }
            mmOutputStream = null;
        }

        if (mmSocket != null) {
            try {
                mmSocket.close();
            } catch (Exception e) {
            }
            mmSocket = null;
        }

    }


    private String receiveData() {


        if (connectionState == BluetoothDevice.ACTION_ACL_CONNECTED) {


            if (mClients.size() > 0) {


                final byte delimiter = 10; //This is the ASCII code for a newline character

                readBufferPosition = 0;
                readBuffer = new byte[64]; //inizialmente a 1024
                stopWorker = false;
 //-------------------------------------------------------------------------------------------------

              while (!stopWorker)
              {
                try {

                    int bytesAvailable = mmInputStream.available();

                    if (bytesAvailable > 0)
                    {
                        byte[] packetBytes = new byte[bytesAvailable];
                        mmInputStream.read(packetBytes);
                        for (int i = 0; i < bytesAvailable; i++)
                        {
                            byte b = packetBytes[i];
                            if (b == delimiter)
                            {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String data = new String(encodedBytes, "US-ASCII");
                                readBufferPosition = 0;
                                return data;
                            }
                            else
                            {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                } catch (IOException ex) {

                    stopWorker = true;
                }
              }
            }
            return "";
        } else {
            try {

                sendMessageToUI(MSG_SET_STRING_VALUE, "Try to connect...");
                connect(this.bTDevice, uuid);
            }
            catch ( Exception e)
            {
                e.printStackTrace();
            }
            eseguiBeepConnessioneRipristinata = true;
            if(eseguiBeepConnessionePersa == true)
            {
                generateBeepSound(250);
                eseguiBeepConnessionePersa=false;
            }

            return "";
        }

    }


    void sendData(String dataToSend) throws IOException //spedisce il dato al dispositivo bt connesso
    {
        String msg =dataToSend;
        if(!dataToSend.equals("")) {
            try {
                mmOutputStream.write(msg.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }

            //myLabel.setText("Data Sent: " + msg);
        }

    }
/*
    void closeBT() throws IOException
    {
       // stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
       // myLabel.setText("Bluetooth Closed");
    }
    */
    private void showPasswordNotification() {
        nm1 = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.password_required);
        // Set the icon, scrolling text and timestamp
        //Notification notification = new Notification(R.drawable.icon, text, System.currentTimeMillis());
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, PasswordActivity.class), 0);
        // Set the info for the views that show in the notification panel.

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notifica_psw)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setContentTitle("BlueMoto").build();
        notification.flags = Notification.FLAG_AUTO_CANCEL; // RENDE LA NOTIFICA NON CANCELLABILE DALL'UTENTE.


        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        nm1.notify(R.string.password_required, notification);
    }

/*

    private void showNotification() {
        nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.service_started);
        // Set the icon, scrolling text and timestamp
        //Notification notification = new Notification(R.drawable.icon, text, System.currentTimeMillis());
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        // Set the info for the views that show in the notification panel.

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notifica)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setContentTitle("BlueMoto").build();
        notification.flags = Notification.FLAG_ONGOING_EVENT; // RENDE LA NOTIFICA NON CANCELLABILE DALL'UTENTE.


        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        nm.notify(R.string.service_started, notification);
    }
*/

    private  Boolean testLastMeasuredTime()
    {
        Boolean risultato = false;
        long tempoRefresh = System.currentTimeMillis();
        if( tempoRefresh > (pastTimeSeconds + REFRESH_TIME_UPDATE_INFO_SPEED) )
        {
            pastTimeSeconds = tempoRefresh;
            risultato  =true;
        }
        else {

            risultato =false;
        }

        return risultato;
    }
    private void elaborateData(String rxData)
    {


       /*if (rxData.startsWith("SP"))
       {
           //Sto ricevendo la velocità reale del veicolo di 3 cifre
           String SP = rxData.substring(2);

           // applico il fattore correttivo prima di far visualizzare il valore della velocità


           Constants.SPEED = Double.parseDouble(SP) * Constants.SPEED_FACTOR; // memorizzo il valore della velocità all'interno della variabile statica dopo aver applicato il fattore correttivo.


           if(Constants.LOGGING_ENABLED == true && Constants.SPEED > Constants.SPEED_MIN_TO_RECORD && testLastMeasuredTime()) // se è abilitato il logging salvo tutte le velocità nel DB
           {
               //salvo la nuova velocità acquisita nel DB

               int speed = (int) Constants.SPEED.floatValue() ;
              // Constants.DBMANAGER.saveSpeed(speed, getDateTime());
           }
           if (Constants.SPEED <= Constants.SPEED_MIN_TO_RECORD) {
               sendMessageToUI(MSG_SET_STRING_VALUE, "SP" + 0);
           } else {
               sendMessageToUI(MSG_SET_STRING_VALUE, "SP" + Constants.SPEED);
           }

           if(testLastMeasuredTime()) {

               sendPollingConection();
           }


       }*/
       if (rxData.startsWith("ST"))
       {
            //Sto ricevendo il timeout di 3 cifre
            String ST = rxData.substring(2,5);

           ST = ST.replaceFirst("^0+(?!$)", "");
            sendMessageToUI(MSG_SET_STRING_VALUE, "ST"+ST);


        }

        if (rxData.startsWith("SS"))
       {
            //Sto ricevendo la velocita di soglia impostata (speed) di 3 cifre
            String SS = rxData.substring(2,5);
           SS = SS.replaceFirst("^0+(?!$)","");
            sendMessageToUI(MSG_SET_STRING_VALUE, "SS"+SS);
       }

        if (rxData.equals("?\r"))
        {
            if(testLastMeasuredTime()) {
                sendPollingConection();
            }
            sendMessageToUI(MSG_SET_STRING_VALUE, "BT connected");
            if(eseguiBeepConnessioneRipristinata == true)
            {
                generateBeepSound(800);
                eseguiBeepConnessioneRipristinata = false; // disabilito la ripetizione del beep in caso di connessione avvenuta
                eseguiBeepConnessionePersa= true; //abilito la generazione del beep in caso di perdita di connessione
            }
        }
        if (rxData.equals("PSW\r"))
        {
            //apro na nuova activity per l'immissione della password

            if(passwordRichiesta == false) {

                generateBeepSound();
                sendMessageToUI(MSG_SET_STRING_VALUE, "PSW");
                passwordRichiesta = true;showPasswordNotification();
                timer.cancel();
                try {
                    Thread.sleep(refreshTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
        if (rxData.startsWith("SLO"))
        {
            String ST ="";
            //Sto ricevendo il timeout di 3 cifre
            if (rxData.length() == 7)
            {
                ST = rxData.substring(3,6);
            }
            else if (rxData.length() == 6)
            {
                ST = rxData.substring(3,5);
            }
            else if (rxData.length() == 5)
            {
                ST = rxData.substring(3,4);
            }


            ST = ST.replaceFirst("^0+(?!$)", ""); //rimpiazza i primi 0 con "".
            sendMessageToUI(MSG_SET_STRING_VALUE, "SLO: "+ST);


        }

        if (rxData.equals("PSW=\r"))
        {
            passwordRichiesta = false;
        }


    }

    private String getDateTime ()
    {
        DateFormat df = new SimpleDateFormat("dd:MMM HH:mm:ss");
        String localTime = df.format(Calendar.getInstance().getTime());

        //Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+1:00"));
        //Date currentLocalTime = cal.getTime();
       // DateFormat date = new SimpleDateFormat("HH:mm:ss");
        // you can get seconds by adding  "...:ss" to it
       // date.setTimeZone(TimeZone.getTimeZone("GMT+1:00"));

        //String localTime = date.format(currentLocalTime);
        return localTime;

    }

    private Boolean generateBeepSound(int duration)//int numberBeep, int durtionSingleBeep, int frequencyBeep)
    {
        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,duration);

       // ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_NOTIFICATION,100);
       // toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
        return true;
    }
    private Boolean generateBeepSound()//int numberBeep, int durtionSingleBeep, int frequencyBeep)
    {
         ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_NOTIFICATION,100);
         toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
        return true;
    }

    private Boolean sendPollingConection()
    {

        try {
            sendData("!\n");

            //variabile che la password spedita è ok!!
            passwordRichiesta = false;
            try {
                nm1.cancel(R.string.password_required);// Cancel the auto-hide notification password icon.
            }
            catch (Exception e)
            {}

        } catch (IOException e) {
           return  false;
        }

        return true;
    }

    public static boolean isRunning()
    {
        return isRunning;
    }

    private void onTimerTick() {
        Log.i("TimerTick", "Timer doing work." + counter++);
        receivedData = receiveData(); //stringa ricevuta fino al carattere ascii "new line"

        new Elabora().run();
    }

    public class Elabora  implements Runnable{


        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            elaborateData(receivedData);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) {timer.cancel();}
        counter=0;
      //  nm.cancel(R.string.service_started); // Cancel the persistent notification.
        try {
            nm1.cancel(R.string.password_required);// Cancel the auto-hide notification password icon.
        }
        catch (NullPointerException e)
        {}
        Log.i("MotoConnectionService", "Service Stopped.");
        isRunning = false;
        resetConnection();
        Constants.SPEED_MAX = 0d;


    }

}