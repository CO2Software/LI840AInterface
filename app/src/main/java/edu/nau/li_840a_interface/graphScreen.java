/*
 *  Author: James Beasley
 *  Last updated: April 6th, 2018
 *  Description: Java file for the graph screen of the application. Contains methods which
 *               correspond to each button on the screen, as well as a receiver for data from the
 *               gas analyzer.
 */
package edu.nau.li_840a_interface;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.jjoe64.graphview.GraphView;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.Set;

public class graphScreen extends AppCompatActivity {

    private GraphView graphIds[];
    private TextView textIds[];
    private GraphManager manager;
    private Thread manager_thread;
    private SerialReader reader;

    /*
     *  Constructor for the graph screen. Fetches the screen IDs for the graphs and text boxes, and
     *  passes them off to the GraphManager to be updated at a rate of once per second.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_screen);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        getWindow().getDecorView().setSystemUiVisibility(
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
                    }
                });

        // Get the screen IDs for each of the four graphs
        graphIds = new GraphView[4];
        graphIds[0] = findViewById(R.id.graph1);
        graphIds[1] = findViewById(R.id.graph2);
        graphIds[2] = findViewById(R.id.graph3);
        graphIds[3] = findViewById(R.id.graph4);

        // Get the screen IDs for each of the four text views
        textIds = new TextView[4];
        textIds[0] = findViewById(R.id.co2display);
        textIds[1] = findViewById(R.id.h2odisplay);
        textIds[2] = findViewById(R.id.tempdisplay);
        textIds[3] = findViewById(R.id.presdisplay);

        // Initialize the handler. This is used for the USB serial communication
        mHandler = new graphScreen.MyHandler(this);

        // Initialize a new serial reader object, for reading in information from the instrument
        reader = new SerialReader();

        // Initialize a new graph manager using the graph and text IDs, and then set it to start
        // updating on its own thread
        manager = new GraphManager(this, graphIds, textIds);
        manager_thread = new Thread(manager);
        manager_thread.start();

    }

    /*
     *  Runs when the "CO2" button is pressed. Hides every graph except for the CO2 one.
     */
    public void showCO2(View view)
    {

        graphIds[0].setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.MATCH_PARENT));
        graphIds[1].setLayoutParams(new LinearLayout.LayoutParams(0, 0));
        graphIds[2].setLayoutParams(new LinearLayout.LayoutParams(0, 0));
        graphIds[3].setLayoutParams(new LinearLayout.LayoutParams(0, 0));


    }

    /*
     *  Runs when the "H2O" button is pressed. Hides every graph except for the H2O one.
     */
    public void showH2O(View view)
    {

        graphIds[1].setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.MATCH_PARENT));
        graphIds[2].setLayoutParams(new LinearLayout.LayoutParams(0, 0));
        graphIds[3].setLayoutParams(new LinearLayout.LayoutParams(0, 0));
        graphIds[0].setLayoutParams(new LinearLayout.LayoutParams(0, 0));

    }

    /*
     *  Runs when the "Temp" button is pressed. Hides every graph except for the temperature one.
     */
    public void showTemp(View view)
    {

        graphIds[2].setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.MATCH_PARENT));
        graphIds[3].setLayoutParams(new LinearLayout.LayoutParams(0, 0));
        graphIds[0].setLayoutParams(new LinearLayout.LayoutParams(0, 0));
        graphIds[1].setLayoutParams(new LinearLayout.LayoutParams(0, 0));

    }

    /*
     *  Runs when the "Pressure" button is pressed. Hides every graph except for the pressure one.
     */
    public void showPres(View view)
    {

        graphIds[3].setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.MATCH_PARENT));
        graphIds[0].setLayoutParams(new LinearLayout.LayoutParams(0, 0));
        graphIds[1].setLayoutParams(new LinearLayout.LayoutParams(0, 0));
        graphIds[2].setLayoutParams(new LinearLayout.LayoutParams(0, 0));

    }

    /*
     *  Runs when either the "Start Logging" or "Stop Logging" buttons are pressed. Switches the
     *  text to the appropriate value and tells the graph manager to either start or stop recording
     *  incoming data points.
     */
    public void startStopLog(View view)
    {

        Button button = findViewById(R.id.logbutton);
        Button finalizeButton = findViewById(R.id.finalbutton);

        // If the button current says "Start Logging", switch the text and inform the manager
        if (button.getText().equals("Start Logging"))
        {
            button.setText("Stop Logging");
            finalizeButton.setBackgroundColor(Color.TRANSPARENT);
            finalizeButton.setEnabled(false);
            manager.resetGraphs();
            manager.startlogging();
        }

        // If the button currently says "Stop Logging", switch the text and inform the manager
        else
        {
            button.setText("Start Logging");
            if (!manager.isEmpty())
            {
                finalizeButton.setBackgroundResource(android.R.drawable.btn_default);
                finalizeButton.setEnabled(true);
            }
            manager.stoplogging();
        }

    }

    /*
     *  Runs when the zoom button is pressed. If it currently says "Enable Zoom", then it tells
     *  the graph manager to enable zoom for each graph. If it current says "Disable Zoom", then it
     *  tells the graph manager to disable zoom for each graph.
     */
    public void zoomToggle(View view)
    {

        Button button = findViewById(R.id.zooombutton);

        // If the button currently says "Enable Zoom"
        if (button.getText().equals("Enable Zoom"))
        {
            button.setText("Disable Zoom");
            manager.enableZoom();
        }

        // If the button currently says "Disable Zoom"
        else
        {
            button.setText("Enable Zoom");
            manager.disableZoom();
        }
    }

    /*
     *  Runs when the "Back" button is pressed. Simply goes back to the metadata screen without
     *  witting any files.
     */
    public void goToMetadata(View view)
    {
        final Context con = this;
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this);
        builder.setTitle("Exit graph screen?");
        builder.setMessage("Are you sure you want to exit the graph screen? You will lose all" +
                " currently recorded data.");
        builder.setIcon(android.R.drawable.ic_dialog_alert);

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent metaDataScreen;
                metaDataScreen = new Intent(con, metaData.class);
                metaDataScreen.putExtra("FLAG", "True");
                startActivity(metaDataScreen);
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        builder.show();
    }

    /*
     *  Runs when the "Finalize" button is pressed. Gets the string values for the graph file,
     *  metadata file, and image file, writes them to files, deconstructs the graph manager, and
     *  then takes the user to the file directory.
     */
    public void finalize(View view)
    {

        String reading;
        String metaSite;
        String metaOpName;
        String metaSampleId;
        String metaTemp;
        String metaComments;
        String metaTime;
        String metaString;
        String metaLong;
        String metaLat;
        String metaElevation;
        Bitmap imageString;
        String metaImageFile;
        String fileName;
        String graphArray[];
        String slope;
        String rSquared;
        String stdError;
        String regSlope;
        FileOutputStream outStream;
        Intent fileScreen;

        Button button = findViewById(R.id.finalbutton);
        button.setText("Saving...");

        // Deconstruct the manager
        manager.stoplogging();
        manager.deconstruct();
        reader.communicating = false;

        // Get the graph CSV from the manager
        reading = manager.toString();

        // Fetch the metadata values
        metaSite = getIntent().getStringExtra("SITE_NAME");
        metaOpName = getIntent().getStringExtra("OPERATOR_NAME");
        metaSampleId = getIntent().getStringExtra("SAMPLE_ID");
        metaTemp = getIntent().getStringExtra("TEMPERATURE");
        metaComments = getIntent().getStringExtra("COMMENTS");
        metaTime = getIntent().getStringExtra("TIME");
        metaLong = getIntent().getStringExtra("GPSLong");
        metaLat = getIntent().getStringExtra("GPSLat");
        metaElevation = getIntent().getStringExtra("ELEVATION");
        //System.out.println("TEST: Getting Bitmap from Extra");
        imageString = getIntent().getParcelableExtra("IMAGE");
        metaImageFile = getIntent().getStringExtra("IMAGEPATH");



        // Split the graph data so that we can only get stats on the CO2 graph
        graphArray = splitGraphData(reading);
        DecimalFormat df = new DecimalFormat("#.0000");

        // Calculate each of the stats
        rSquared = df.format(getRSquared(graphArray[0]));
        stdError = df.format(getStandardError(graphArray[0]));
        regSlope = df.format(getRegressionSlope(graphArray[0]));

        // Construct the CSV file content
        metaString = "Operator Name,Site Name,Sample ID,Temperature,Comments,Time and Date,Longitude," +
                     "Latitude,Elevation,R Squared,Regression Slope,Standard Error\n" +
                     metaOpName + "," + metaSite + "," + metaSampleId + "," + metaTemp + "," +
                     metaComments + "," + metaTime + "," + metaLong + "," + metaLat + "," +
                     metaElevation + "," + rSquared + "," + regSlope + "," + stdError;


        // Build the file name using the site name, sample id, and time stamp
        fileName = metaSite + "_" + metaSampleId + "_" + metaTime;

        // In case any file I/O exception happen, we write this in a try/catch block
        try
        {
            // Graph File
            outStream = openFileOutput("G-" + fileName + ".csv", Context.MODE_APPEND);
            outStream.write(reading.getBytes());
            outStream.close();

            // Metadata File
            outStream = openFileOutput("M-" + fileName + ".csv", Context.MODE_APPEND);
            outStream.write(metaString.getBytes());
            outStream.close();

            // Image File, only if an image was taken
            if (imageString != null)
            {
                System.out.println("TEST: Converting File to PNG");
                outStream = openFileOutput("I-" + fileName + ".png", Context.MODE_APPEND);
                //Bitmap imageBMP = BitmapFactory.decodeFile(metaImageFile);
                imageString.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                //outStream.write(imageString.getBytes());
                outStream.close();
            }

        }
        catch(Exception exception)
        {

        }

        // Flag for Meta Data Refill = False
        // intent.putExtra("FLAG", "False")

        // Initialize a new file screen and take the user there
        fileScreen = new Intent(this, fileDirectory.class);
        startActivity(fileScreen);

    }


    /////////////////////
    // USB SERIAL CODE //
    //  DO NOT MODIFY  //
    /////////////////////

    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    if (usbService != null)
                    {
                        String initMessage;
                        initMessage = "<LI840><CFG><OUTRATE>0.5</OUTRATE></CFG></LI840>\n";
                        usbService.write(initMessage.getBytes());
                    }
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private UsbService usbService;
    private graphScreen.MyHandler mHandler;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<graphScreen> mActivity;

        public MyHandler(graphScreen activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    mActivity.get().reader.addChar(data);
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    ////////////////////////////
    // END OF USB SERIAL CODE //
    ////////////////////////////

    /////////////////////
    // PRIVATE CLASSES //
    /////////////////////

    /*
     *  Class used to communicate with the USB serial connection. More or less, it is just a really
     *  glorified string builder.
     */
    private class SerialReader
    {

        public String currentStream;
        public String completeStream;
        public boolean communicating;

        /*
         *  Constructor for the serial reader. Initializes all the class member variables.
         */
        public SerialReader()
        {
            currentStream = "";
            completeStream = "";
            communicating = true;
        }

        /*
         *  This method is called every time the USB port picks up a piece of data from the
         *  instrument. Makes sure it is not empty or a space, then adds it to the string being
         *  built. If the message contains a line break, then we know it is the end of the current
         *  message.
         */
        public void addChar(String input)
        {

            // Check to make sure its not a space or ampty
            if (input.equals("") || input.equals(" "))
            {
                return;
            }

            // If it contains a line break, then we know we have reached the end of the current
            // message. Send the complete message to the graph manager.
            if (input.contains("\n"))
            {
                currentStream += input;
                completeStream = currentStream.trim();
                currentStream = "";
                manager.updateData(completeStream);
            }

            // If it does not contain a line break, then we just add the message to the string
            // being built.
            else
            {
                currentStream += input;
            }

        }

    }

    /*
     * If the Android back button is pressed, we don't want anything to happen since all of our
     * app nagivation is handled by on screen buttons.
     */
    @Override
    public void onBackPressed()
    {

    }

    // JOEY ADDED STATISTICS HERE TO BE ABLE TO WRITE TO THE CSV FILE
    private String[] splitGraphData(String graphFileContents)
    {

        String lines[];
        String values[];
        String output[];
        String co2Points;
        String h2oPoints;
        String tempPoints;
        String presPoints;
        int count;

        co2Points = "";
        h2oPoints = "";
        tempPoints = "";
        presPoints = "";

        lines = graphFileContents.split("\n");

        for (count = 1; count < lines.length; count++)
        {

            values = lines[count].split(",");

            co2Points += values[0] + "," + values[1] + "\n";
            h2oPoints += values[0] + "," + values[2] + "\n";
            tempPoints += values[0] + "," + values[3] + "\n";
            presPoints += values[0] + "," + values[4] + "\n";

        }

        output = new String[4];

        output[0] = co2Points;
        output[1] = h2oPoints;
        output[2] = tempPoints;
        output[3] = presPoints;

        return output;

    }

    private double getStandardError(String graphPoints){

        String[] data;
        String[] tempData;
        int numOfPoints;

        if (graphPoints == ""){
            return 0;
        }

        SimpleRegression SR = new SimpleRegression();


        //split data
        data = graphPoints.split("\n");
        numOfPoints = data.length;

        //loop through and get mean of all points
        for(int i = 0; i < numOfPoints; i++){
            tempData = data[i].split(",");
            SR.addData( Double.parseDouble(tempData[0]),  Double.parseDouble(tempData[1]));
        }

        return SR.getSlopeStdErr();


    }

    private double getRegressionSlope(String graphPoints){

        String[] data;
        String[] tempData;
        int numOfPoints;

        if (graphPoints == ""){
            return 0;
        }

        SimpleRegression SR = new SimpleRegression();


        //split data
        data = graphPoints.split("\n");
        numOfPoints = data.length;

        //loop through and get mean of all points
        for(int i = 0; i < numOfPoints; i++){
            tempData = data[i].split(",");
            SR.addData( Double.parseDouble(tempData[0]),  Double.parseDouble(tempData[1]));
        }

        return SR.getSlope();
    }

    private double getYIntercept(String graphPoints){

        String[] data;
        String[] tempData;
        int numOfPoints;

        if (graphPoints == ""){
            return 0;
        }

        SimpleRegression SR = new SimpleRegression();


        //split data
        data = graphPoints.split("\n");
        numOfPoints = data.length;

        //loop through and get mean of all points
        for(int i = 0; i < numOfPoints; i++){
            tempData = data[i].split(",");
            SR.addData( Double.parseDouble(tempData[0]),  Double.parseDouble(tempData[1]));
        }

        return SR.getIntercept();


    }

    private double getRSquared(String graphPoints){

        double xTotal = 0;
        double yTotal = 0;
        double xSquaredTotal = 0;
        double ySquaredTotal = 0;
        double XY = 0;
        double rSquared = 0;
        String[] data;
        String[] tempData;
        int numOfPoints;
        double tempX;
        double tempY;

        if (graphPoints == ""){
            return 0.0;
        }

        //split data
        data = graphPoints.split("\n");
        numOfPoints = data.length;

        //loop through and get xtotal and ytotal and their squares
        for(int i = 0; i < numOfPoints; i++){
            tempData = data[i].split(",");

            tempX = Double.parseDouble(tempData[0]);
            tempY = Double.parseDouble(tempData[1]);

            //get x and y
            xTotal += tempX;
            yTotal += tempY;

            //get x and y rSquared
            xSquaredTotal += (tempX * tempX);
            ySquaredTotal += (tempY * tempY);

            //get sum of x*y
            XY += (tempX * tempY);
        }

        //rsquared equation
        rSquared = (((numOfPoints * XY) - (xTotal * yTotal)) /
                ((Math.sqrt((numOfPoints * xSquaredTotal) - (xTotal*xTotal))) * (Math.sqrt((numOfPoints * ySquaredTotal) - (yTotal*yTotal)))));

        //square r
        rSquared  = (rSquared * rSquared);

        return rSquared;
    }
    // END OF JOEYS CODE ADDING

}
