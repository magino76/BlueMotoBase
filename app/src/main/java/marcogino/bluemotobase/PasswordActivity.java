package marcogino.bluemotobase;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

//import marcogino.bluemoto.MotoConnectionService;

/**
 * Created by Marco_76 on 02/06/2016.
 */
public class PasswordActivity extends AppCompatActivity {

    String extra;
    EditText et;
    Button bt;
    boolean mIsBound;
    Messenger mService = null;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String str1="";
            switch (msg.what) {


                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_psw);
        Intent intent = getIntent();
        //extra = intent.getStringExtra("PSW");

        et = (EditText) findViewById(R.id.password);

        bt = (Button)findViewById(R.id.ok);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPassword();
            }
        });
        CheckIfServiceIsRunning();


       // et.setText(extra);
    }
    @Override
    protected  void onStop()
    {
       //MotoConnectionService.rxOn=true;
        super.onStop();

    }


    void doBindService() {
        bindService(new Intent(this, MotoConnectionService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;

        //MotoConnectionService.rxOn = false;
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


        }
    };
    private void setPassword ()
    {
        try {



            if ((Integer.parseInt(et.getText().toString()) <= 999999) && (!et.getText().toString().equals(""))) {

                sendPswToService(et.getText().toString()); //

               // Intent returnIntent = new Intent();

                //returnIntent.putExtra("result", et.getText().toString());
                //setResult(Activity.RESULT_OK, returnIntent);
                finish();


            } else {
                et.setBackgroundColor(Color.red(255));
                finish();
            }
        }
        catch ( Exception e)
        {
            finish();
        }
    }
    private void CheckIfServiceIsRunning() {
        //If the service is running when the activity starts, we want to automatically bind to it.
        if (MotoConnectionService.isRunning()) { //Utilizzo il metodo statico per testarne lo stato.
            doBindService();

        }
    }





}
