package marcogino.bluemotobase;

import android.app.Activity;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    SharedPreferences sharedPref;
    int numero;
    TextView messaggio;
    TextView twSpeed;
    TextView twSpeedMax;
    TextView dataDeviceToConnect;
    TextView kfactor;
    Button btConnetti;
    Button btDisconnetti;
    Messenger mService = null;
    boolean mIsBound;
    String json;
    Double velocitaMax= 0d;
    int speedM;
    SharedPreferences prefs;
    @Override
    protected void onStop()
    {
        super.onStop();

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        messaggio = (TextView)findViewById(R.id.messaggio);
       // twSpeed = (TextView)findViewById(R.id.twSpeed);
        ///kfactor = (TextView)findViewById(R.id.kfactor);

        btConnetti = (Button) findViewById(R.id.btConnetti);
        btDisconnetti = (Button) findViewById(R.id.btDisconnetti);
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        dataDeviceToConnect = (TextView)findViewById(R.id.dataDeviceToConnect);
        //twSpeedMax = (TextView)findViewById(R.id.twSpeedMax);
        dataDeviceToConnect.setText(writeTargetDeviceOnLabel());

        CheckIfServiceIsRunning();
        readSpeedFactor();

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        try {
            dataDeviceToConnect.setText(writeTargetDeviceOnLabel());
        }
        catch (Exception e)
        {}
    }

    /**
     * Reference to a Handler, which others can use to send messages to it.
     * This allows for the implementation of message-based communication across
     * processes, by creating a Messenger pointing to a Handler in one process,
     * and handing that Messenger to another process.
     * */
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
                        //messaggio.setText(str1);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    if(str1.startsWith("ST"))
                    {
                        //TimeoutToSend.setText(str1.substring(2));
                    }
                    if(str1.startsWith("SS"))
                    {
                        //SpeedToSend.setText(str1.substring(2));
                    }
                    if(str1.startsWith("SP"))
                    {
                       // speedMax(str1);

                    }

                    if(str1.equals("PSW"))
                    {
                        richiestaPassword();
                    }
                   /* if(str1.equals("StopService"))
                    {
                        doUnbindService();
                        Intent stopIntent = new Intent(MainActivity.this, MotoConnectionService.class);
                        stopIntent.setAction(Constants.STOPFOREGROUND_ACTION);
                        stopService(stopIntent);
                    }
                    */
                    else if(str1.equals("BT connected"))
                    {
                        //eseguo se è connesso...
                        messaggio.setText(str1);
                    }
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }


    private void speedMax (String val)
    {

        Double velocita = Double.parseDouble(val.substring(2));
        velocitaMax = Constants.SPEED_MAX;
        if(velocita > velocitaMax )
        {
            velocitaMax = Constants.SPEED_MAX = velocita;

        }

        speedM = (int) velocitaMax.floatValue();
        int speed = (int) velocita.floatValue();

        twSpeed.setText(Integer.toString(speed));
        twSpeedMax.setText(Integer.toString(speedM));



    }

    private String writeTargetDeviceOnLabel()
    {
        try {
            prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE);

            json = prefs.getString("BtDeviceName", "Device not found");

            Constants.BT_DEVICE_NAME_SELECTED=json;


        }
        catch (Exception e)
        {}


        return json;
    }

    private void richiestaPassword()
    {
        // MotoConnectionService.rxOn=true;

        //la prima volta che apro activity password setto un boolean a true fino a qu

        Intent intent = new Intent(MainActivity.this, PasswordActivity.class);
        //startActivityForResult(intent, Constants.PASSWORD_ACTIVITY);// Activity is started with requestCode  "Constants.PASSWORD_ACTIVITY"
        startActivity(intent);// Activity is started

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode ==  Constants.PASSWORD_ACTIVITY) {
            if(resultCode == Activity.RESULT_OK){
                String  result = data.getStringExtra("result");
                // stringa di 6 cifre da inoltrare al servizio il quale la inoltrerà al BT device

                //MotoConnectionService.rxOn= true;
                sendPswToService(result);


            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
                //Nessuna password inserita

                // MotoConnectionService.rxOn= true;
            }
        }
    }//onActivityResult
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service); // restituisce istanza del servizio connesso
            messaggio.setText("Attached");
            try {
                Message msg = Message.obtain(null, MotoConnectionService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger; // mMessenger:  Riferimento a un gestore , che altri possono utilizzare per inviare messaggi ad esso .
                mService.send(msg); //mService: istanza del servizio ottenuta dal binding
            }
            catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
                messaggio.setText(e.toString());
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
            messaggio.setText("Disconnected");

        }
    };
    private void CheckIfServiceIsRunning() {
        //If the service is running when the activity starts, we want to automatically bind to it.
        if (MotoConnectionService.isRunning()) { //Utilizzo il metodo statico per testarne lo stato.
            doBindService();

        }
    }


    public void btPressed(View view)
    {
        switch (view.getId())
        {

            case R.id.btConnetti:
                if (!MotoConnectionService.isRunning()) {

                    Intent startIntent = new Intent(Constants.STARTFOREGROUND_ACTION);
                    startIntent.setClass(MainActivity.this, MotoConnectionService.class);
                    startService(startIntent);

                    //startService(new Intent(MainActivity.this, MotoConnectionService.class));

                    doBindService();
                }
                break;
            case R.id.btDisconnetti:
                if (MotoConnectionService.isRunning()) { //Utilizzo il metodo statico per testarne lo stato.

                    doUnbindService();
                    Intent stopIntent = new Intent(MainActivity.this, MotoConnectionService.class);
                    stopIntent.setAction(Constants.STOPFOREGROUND_ACTION);
                    stopService(stopIntent);


                   // stopService(new Intent(MainActivity.this, MotoConnectionService.class));
                }
                break;


           // case R.id.resetSpeedMax:

             //   Constants.SPEED_MAX = 0;  // reset della velocità massima memorizzata
             //   twSpeedMax.setText(Double.toString(Constants.SPEED_MAX));
             //   break;

        }

    }

    private Boolean readSpeedFactor()
    {
        SharedPreferences prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        Constants.SPEED_FACTOR = Double.parseDouble( prefs.getString("SpeedFactor", "1.0" ));

        return true;
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

    void doBindService() {
        bindService(new Intent(this, MotoConnectionService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        messaggio.setText("Binding");
        //MotoConnectionService.rxOn = false;
    }


    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, MotoConnectionService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);

                }
                catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            messaggio.setText("Unbinding...");
            stopService(new Intent(MainActivity.this, MotoConnectionService.class));
            messaggio.setText("Stop Service");
            //twSpeed.setText("---");

        }
    }


    public boolean connectedToService()
    {
        Constants.CONNECTED_TO_DEVICE = true; // true se connesso al dispositivo (NON al servizio!!)


        return Constants.CONNECTED_TO_DEVICE;
    }
    public int convertSpeedToInt(String testo)
    {
        if(testo.length()!= 0 )
        {
            try {numero = Integer.parseInt(testo);
                if(Constants.SPEED_MIN > numero || numero > Constants.SPEED_MAX)
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






    @Override
    public boolean onCreateOptionsMenu(Menu menu) { //viene lanciato quando il menu options menù è stato creato
        getMenuInflater().inflate(R.menu.menu_bt_device, menu); //prende il layout dell'oggetto menu e lo mette in menu.

        return true; //true per farlo apparire.
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {


        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getTitle().toString())
        {
            case "Settings":
                 intent = new Intent(MainActivity.this, SettingsActivity.class);
                // intent.putExtra("recordId", i);
                startActivity(intent);
                break;
        }




        return true;

    }

    private void sendMessageToService(int intvaluetosend) {
        if (mIsBound) { // se il servizio  è connesso...
            if (mService != null) { //se l'istanza del servizio non è null...
                try {
                    Message msg = Message.obtain(null, MotoConnectionService.MSG_SET_INT_VALUE,intvaluetosend,0);//Message.obtain: setta il valore di alcuni parametri
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                }
            }
        }
    }

    private void sendConfigToService(String strinvaluetosend) {
        if (mIsBound) { // se il servizio  è connesso...
            if (mService != null) { //se l'istanza del servizio non è null...
                try {


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
    private void sendPswToService(String strinvaluetosend) {
        if (mIsBound) { // se il servizio  è connesso...
            if (mService != null) { //se l'istanza del servizio non è null...
                try {

                    Message msg = Message.obtain(null, MotoConnectionService.MSG_SET_PSW_VALUE);
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


}
