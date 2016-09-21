package marcogino.bluemotobase;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

//import marcogino.bluemoto.Constants;
//import marcogino.bluemoto.MotoConnectionService;

/**
 * Created by Marco_76 on 21/08/2016.
 */


public class SettingsActivity extends AppCompatActivity {


    class RptUpdater implements Runnable {
        public void run() {
            if( mAutoIncrement ){
                increment();
                repeatUpdateHandler.postDelayed( new RptUpdater(), REP_DELAY );
            } else if( mAutoDecrement ){
                decrement();
                repeatUpdateHandler.postDelayed( new RptUpdater(), REP_DELAY );
            }
        }



    }

    public void decrement()
    {
        if(Constants.SPEED_FACTOR >= 0.2d)
        {
            Constants.SPEED_FACTOR = Constants.SPEED_FACTOR - 0.01d;
            updateSpeedFactor(Constants.SPEED_FACTOR); // memorizzo la variabile statica nel sistema...
            kfactor.setText(Constants.SPEED_FACTOR.toString());
        }


    }
    public void increment()
    {
        if(Constants.SPEED_FACTOR<10.0f)
        {
            Constants.SPEED_FACTOR = Constants.SPEED_FACTOR + 0.01f;
            updateSpeedFactor(Constants.SPEED_FACTOR); // memorizzo la variabile statica nel sistema...
            kfactor.setText(Constants.SPEED_FACTOR.toString());
        }
    }


    private static final int REP_DELAY = 100;

    BluetoothDevice mmDevice;

    TextView messaggio;
    Spinner spinnerDevice;
    TextView TimeoutToSend;
    TextView SpeedToSend;
    TextView SpeedSetted;
    TextView kfactor;
    TextView labelLogging;
    Switch switchEnableLogging;
    Button btTimeout;
    Button btSetSpeed;
    Button btGetSetSpeed;
    Button btSpeedUp;
    Button btSpeedDown;
    Button btOK;
    List<String> lables;
    int numero;
    boolean mIsBound;
    ArrayList<BluetoothDevice> listDevices = null;
    Messenger mService = null;
    BluetoothAdapter mBluetoothAdapter;
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service); // restituisce istanza del servizio connesso

            try {
                Message msg = Message.obtain(null, MotoConnectionService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger; // mMessenger:  Riferimento a un gestore , che altri possono utilizzare per inviare messaggi ad esso .
                mService.send(msg); //mService: istanza del servizio ottenuta dal binding
            }
            catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it

            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
            messaggio.setText("Disconnected.");

        }
    };

    //-------------------------------------- REPEAT --------------------------------
    private Handler repeatUpdateHandler = new Handler();
    private boolean mAutoIncrement = false;
    private boolean mAutoDecrement = false;







    final Messenger mMessenger = new Messenger(new IncomingHandler());

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String str1="";
            switch (msg.what) {
                case MotoConnectionService.MSG_SET_INT_VALUE:
                    messaggio.setText("Int Message: " + msg.arg1);
                    break;
                case MotoConnectionService.MSG_SET_STRING_VALUE:
                    try
                    {
                        Bundle b = (Bundle)msg.getData();
                        str1 = b.get("str1").toString();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    //messaggio.setText(str1);
                    if(str1.startsWith("ST"))
                    {
                        TimeoutToSend.setText(str1.substring(2));
                    }
                    if(str1.startsWith("SS"))
                    {
                        SpeedToSend.setText(str1.substring(2));
                    }
                    if(str1.startsWith("SP"))
                    {
                        //  twSpeed.setText(str1.substring(2));
                    }
                    if(str1.startsWith("SLO"))
                    {

                        Double sloSpeed =  (Integer.parseInt(str1.substring(5)))* Constants.SPEED_FACTOR;

                        SpeedSetted.setText(sloSpeed.toString());

                    }

                    if(str1.equals("PSW"))
                    {
                        //  richiestaPassword();
                    }
                    else if(str1.equals("BT connesso"))
                    {
                        //eseguo se è connesso...
                    }
                    break;
                // case Constants.PASSWORD_ACTIVITY: //Richiesta password
                //  messaggio.setText("PSW");
                //MotoConnectionService.rxOn=false;
                //richiestaPassword();
                //break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void CheckIfServiceIsRunning() {
        //If the service is running when the activity starts, we want to automatically bind to it.
        if (MotoConnectionService.isRunning()) { //Utilizzo il metodo statico per testarne lo stato.
            doBindService();

        }
    }
    void doBindService() {
        bindService(new Intent(this, MotoConnectionService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;


    }


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        messaggio = (TextView)findViewById(R.id.messaggio);
        spinnerDevice = (Spinner)findViewById(R.id.spinnerDevice);
        TimeoutToSend = (EditText)findViewById(R.id.TimeoutToSend);
        // TimeoutToSend.clearFocus();
        SpeedToSend = (EditText)findViewById(R.id.SpeedToSend);
        // SpeedToSend.clearFocus();
        SpeedSetted = (EditText)findViewById(R.id.SpeedSetted);
        //SpeedSetted.clearFocus();
        //kfactor = (TextView)findViewById(R.id.kfactor);

        btTimeout = (Button) findViewById(R.id.btTimeout);
        btSetSpeed = (Button) findViewById(R.id.btSetSpeed);
        btGetSetSpeed = (Button) findViewById(R.id.btGetSetSpeed);
        //btSpeedUp = (Button) findViewById(R.id.btSpeedUp);
        //btSpeedDown = (Button) findViewById(R.id.btSpeedDown);
        btOK = (Button) findViewById(R.id.btOK);

       // kfactor.setText(Constants.SPEED_FACTOR.toString());


        spinnerDevice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

                mBluetoothAdapter.startDiscovery(); //se l'ho selezionato, smetto di cercare....
                mmDevice = (BluetoothDevice)listDevices.get(arg2);
                Constants.BT_DEVICE = mmDevice;
                Constants.BT_DEVICE_NAME_SELECTED = mmDevice.getName().toString();

                SharedPreferences prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE);
                // SharedPreferences.Editor editor = prefs.edit();
                //    editor.putString("BtDevice", mmDevice.toString());
                //    editor.commit();

                //Per salvare oggetti invece uso la libreria json:

                SharedPreferences.Editor prefsEditor = prefs.edit();
                Gson gson = new Gson();
                String json = gson.toJson(mmDevice); // myObject - instance of MyObject
                prefsEditor.putString("BtDevice", json);
                prefsEditor.commit();



                prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE);
                // SharedPreferences.Editor editor = prefs.edit();
                //    editor.putString("BtDevice", mmDevice.toString());
                //    editor.commit();

                //Per salvare oggetti invece uso la libreria json:



                prefsEditor.putString("BtDeviceName", Constants.BT_DEVICE_NAME_SELECTED);
                prefsEditor.commit();



                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID




            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }

        });

        try {
            findBT();
        }
        catch (Exception e)
        {}

        // Spinner Drop down elements


        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, lables);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        try {
            spinnerDevice.setAdapter(dataAdapter);
        }
        catch (Exception e)
        {}

        //Ogni volta che apro l'activity setting lo spinner verrà precaricato con il valore del
        //device BT attualmente selezionato.

        for (int i = 1; i< dataAdapter.getCount(); i++)
        {
            if(dataAdapter.getItem(i).toString().equals(Constants.BT_DEVICE_NAME_SELECTED))
            {
              spinnerDevice.setSelection(i); // preseleziono valore nello spinner.
            }
        }

        CheckIfServiceIsRunning();
    }








    public void btPressed(View view)
    {
        switch (view.getId())
        {
            case R.id.btTimeout:
                if(connectedToService()) {
                    if (convertTimeoutToInt(TimeoutToSend.getText().toString().trim()) != Constants.ERROR_CODE) {
                        //Se sono qui ho premuto il tasto SendTimeout e il numero inserito è corretto
                        sendConfigToService("ST"+TimeoutToSend.getText().toString().trim());
                        TimeoutToSend.setText("");
                    }
                }
                break;
            case R.id.btSetSpeed:
                if(connectedToService())
                {
                    int speed = convertSpeedToInt(SpeedToSend.getText().toString().trim());

                    if (speed != Constants.ERROR_CODE) {

                        //Double Kspeed = (speed / Constants.SPEED_FACTOR);
                        //speed = (int) Kspeed.floatValue();
                        //divido il valore immesso per il fattore di correzzione applicato in fase di decodifica della velocità
                        sendConfigToService("SS"+speed);
                        SpeedToSend.setText("");

                    }
                }
                break;
            case R.id.btGetSetSpeed:
                if(connectedToService())
                {
                    sendConfigToService("SLO");
                }
                break;


            case R.id.btOK:

                this.finish();


                break;

        }

    }

    private void sendConfigToService(String strinvaluetosend) {
        if (mIsBound) { // se il servizio  è connesso...
            if (mService != null) { //se l'istanza del servizio non è null...
                try {
                    // Message msg = Message.obtain(null,MotoConnectionService.MSG_SET_INT_VALUE,intvaluetosend,0);//Message.obtain: setta il valore di alcuni parametri
                    //msg.replyTo = mMessenger;
                    // mService.send(msg);

                    Message msg = Message.obtain(null, MotoConnectionService.MSG_SET_DATA_VALUE);
                    Bundle b = new Bundle();
                    b.putString("str1", strinvaluetosend);
                    msg.setData(b);
                    mService.send(msg);
                }
                catch (RemoteException e) {
                }
            }
        }
    }
    public int convertSpeedToInt(String testo)
    {
        if(testo.length()!= 0 )
        {
            try {numero = Integer.parseInt(testo);
                if(Constants.SET_SPEED_MIN > numero || numero > Constants.SET_SPEED_MAX)
                {
                    numero = Constants.ERROR_CODE;
                    Toast.makeText(this, "La velocità può essere impostata da un minimo di " +
                            Constants.SET_SPEED_MIN  + " ad un massimo di " +
                            Constants.SET_SPEED_MAX +"!"   , Toast.LENGTH_LONG).show();
                }


            } catch (NumberFormatException e) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                numero = Constants.ERROR_CODE;
            }
        }
        else {
            Toast.makeText(this, "Inserire una cifra!", Toast.LENGTH_LONG).show();
            numero = Constants.ERROR_CODE;
        }
        return numero;
    }
    public boolean connectedToService()
    {
        Constants.CONNECTED_TO_DEVICE = true; // true se connesso al dispositivo (NON al servizio!!)


        return Constants.CONNECTED_TO_DEVICE;
    }


    public int convertTimeoutToInt(String testo)
    {
        if(testo.length()!= 0 )
        {
            try {numero = Integer.parseInt(testo);
                if(Constants.TIMEOUT_MIN > numero || numero > Constants.TIMEOUT_MAX)
                {
                    numero = Constants.ERROR_CODE;
                    Toast.makeText(this, "La velocità può essere impostata da un minimo di " +
                            Constants.TIMEOUT_MIN  + " ad un massimo di " +
                            Constants.TIMEOUT_MAX +"!"   , Toast.LENGTH_LONG).show();
                }


            } catch (NumberFormatException e) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                numero = Constants.ERROR_CODE;
            }
        }
        else {
            Toast.makeText(this, "Inserire una cifra!", Toast.LENGTH_LONG).show();
            numero = Constants.ERROR_CODE;
        }
        return numero;
    }

    private Boolean updateSpeedFactor(Double factor)
    {
        /*Per scrivere in un shared preference:

        * SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(getString(R.string.saved_high_score), newHighScore);
        editor.commit();

        Per leggere:

        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        int defaultValue = getResources().getInteger(R.string.saved_high_score_default);
        long highScore = sharedPref.getInt(getString(R.string.saved_high_score), defaultValue);
        */
        //Creo una shared preference di nome "Setting" che conterrà i dati della mia APP.
        //Il primo dato key --> value è il mac address del io device d connettere allo startup del servizio (key: "BtDevice" e mac : "xx:xx:xx:xx:xx:xx"
        //Il dato sarà accessibile da qualsiasi punto della mia app.

        SharedPreferences prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        // SharedPreferences.Editor editor = prefs.edit();
        //    editor.putString("BtDevice", mmDevice.toString());
        //    editor.commit();

        //Per salvare oggetti invece uso la libreria json:

        SharedPreferences.Editor prefsEditor = prefs.edit();

        prefsEditor.putString("SpeedFactor", Constants.SPEED_FACTOR.toString());
        prefsEditor.commit();

        return true;
    }

    void findBT()
    {


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            messaggio.setText("No bluetooth adapter available");
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
        //ArrayAdapter<String> elenco = new ArrayAdapter<String>(this, R.layout.row_layout);

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            // creo una vista di tutti i dispositivi bt presenti e permetto la selezione di uno che verrà memorizzato
            //in un DB
            listDevices = new ArrayList<>();
            lables = new ArrayList<>();
            int i = 0;
            listDevices.clear();
            lables.clear();
            for(BluetoothDevice device : pairedDevices)
            {
                listDevices.add(i,device); //aggiungo ogni device BT accoppiato alla lista
                lables.add(i,device.getName());

//
                //               String name = device.getName();
                //               if(name.equals("GINOM4"))
                //              {
                //                  mmDevice = device;
                ///                   break;
                //              }

                i++;
            }
        }

    }




}

