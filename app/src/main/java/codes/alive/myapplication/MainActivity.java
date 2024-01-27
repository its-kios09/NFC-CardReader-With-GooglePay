package codes.alive.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.devnied.emvnfccard.exception.CommunicationException;
import com.github.devnied.emvnfccard.model.EmvCard;
import com.github.devnied.emvnfccard.parser.EmvTemplate;
import com.github.devnied.emvnfccard.parser.IProvider;
import com.google.gson.Gson;

import net.sf.scuba.util.Hex;

import java.io.IOException;
import java.text.SimpleDateFormat;

import codes.alive.myapplication.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "CARD-Detail";
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private String[][] techListArray;
    private IntentFilter[]intentFiltersArray;

    private ActivityMainBinding binding;

    String amount;
    Uri uri;
    String name = "Fredrick Kioko";
    String upiId = "KilonzoKioko10@gmail.com";
    String transactionNote = "Software Developer | CPU Intern";
    String status;

    public static final String GOOGLE_PAY_PACKAGE_NAME = "com.google.android.apps.nbu.paisa.user";
    int GOOGLE_PAY_REQUEST_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        txtCard=findViewById(R.id.txtCard);
        binding.googlePayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                amount = binding.amountEditText.getText().toString();

                if (!amount.isEmpty()){
                    uri = getUpiPaymentUri(name, upiId, transactionNote, amount);
                    payWithGpay();
                }else{
                    binding.amountEditText.setError("Please enter the amount to pay");
                    binding.amountEditText.requestFocus();
                }
            }
        });

        nfcAdapter=NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            Toast.makeText(this,"NFC Hardware not available on Device",Toast.LENGTH_SHORT).show();
        } else if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this,"NFC is NOT Enabled, Please Enable NFC",Toast.LENGTH_SHORT).show();
        }

        Intent intent=new Intent(this,getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        pendingIntent=PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_MUTABLE);

        intentFiltersArray=new IntentFilter[]{new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)};

        String[] ss=new String[1];
        techListArray=new String[1][];
        ss[0]= NfcA.class.getName();
        techListArray[0]=ss;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter!=null){
            nfcAdapter.enableForegroundDispatch(MainActivity.this,pendingIntent,intentFiltersArray,techListArray);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter!=null){
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    private TextView txtCard;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tagFromIntent=intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        IsoDep tag = IsoDep.get(tagFromIntent);

        try {

        tag.connect();

        IProvider provider=new Provider(tag);

        EmvTemplate.Config config = EmvTemplate.Config()
                .setContactLess(true) // Enable contact less reading (default: true)
                .setReadAllAids(true) // Read all aids in card (default: true)
                .setReadTransactions(true) // Read all transactions (default: true)
                .setReadCplc(false) // Read and extract CPCLC data (default: false)
                .setRemoveDefaultParsers(false) // Remove default parsers for GeldKarte and EmvCard (default: false)
                .setReadAt(true) // Read and extract ATR/ATS and description
                ;
// Create Parser
        EmvTemplate parser = EmvTemplate.Builder() //
                .setProvider(provider) // Define provider
                .setConfig(config) // Define config
//                .setTerminal(terminal) (optional) you can define a custom terminal implementation to create APDU
                .build();

// Read card
            EmvCard card = parser.readEmvCard();


            String ss="";
            ss+=card.getCardNumber()+"\n";
            ss+=card.getApplications().get(0).getApplicationLabel()+"\n";
            ss+=card.getType().getName()+"\n";

            SimpleDateFormat sdf=new SimpleDateFormat("MM-yyyy");
            ss+=sdf.format(card.getExpireDate());

            txtCard.setText(txtCard.getText()+"\n\n"+ss);

//            SystemClock.sleep(1000);

//            tag.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            try {
                tag.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private void payWithGpay(){
        if (isAppInstalled(this, GOOGLE_PAY_PACKAGE_NAME)){
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);
            intent.setPackage(GOOGLE_PAY_PACKAGE_NAME);
            startActivityForResult(intent, GOOGLE_PAY_REQUEST_CODE);
        }else{
            Toast.makeText(MainActivity.this, "Please install Google Pay App", Toast.LENGTH_SHORT).show();
        }
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(data != null){
            status = data.getStringExtra("Status").toLowerCase();
        }
        Log.d("transaction log:- ", "onActivity" + requestCode + RESULT_OK);
        if((RESULT_OK == resultCode) && status.equals("success")){
            Toast.makeText(MainActivity.this, "Completed Transaction successfully", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(MainActivity.this, "Could not transact Please try later", Toast.LENGTH_SHORT).show();

        }
    }
    private static boolean isAppInstalled(Context context, String packageName){
        PackageManager packageManager = context.getPackageManager();
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }


    private static Uri getUpiPaymentUri(String name, String upiId, String transactionNote, String amount){
            return new Uri.Builder()
                    .scheme("upi")
                    .authority("pay")
                    .appendQueryParameter("pa", upiId)
                    .appendQueryParameter("pn", name)
                    .appendQueryParameter("tn", transactionNote)
                    .appendQueryParameter("am", amount)
                    .appendQueryParameter("cu","USD")
                    .build();
    }

}