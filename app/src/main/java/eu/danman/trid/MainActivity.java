package eu.danman.trid;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.format.Time;
import android.util.FloatMath;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.List;

import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;


public class MainActivity extends ActionBarActivity {

    static TextView text;

    private Camera mCamera;
    private CameraPreview mPreview;
    private ImageView capture;
    private int refColor = 0xffff0000;
    private Bitmap bitmap;
    private Spinner resolutionView;
    private String[] resolutions;
    List<Camera.Size> sizes;
    Bitmap processed;
    ProgressBar progressbar;
    int picTaken;
    int toTake;
    EditText editAngle;
    int mode;
    int center;
    int[][] rawData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //set content view AFTER ABOVE sequence (to avoid crash)
        this.setContentView(R.layout.activity_main);

        text = (TextView) this.findViewById(R.id.textView);

        capture = (ImageView) this.findViewById(R.id.imageView);

        progressbar = (ProgressBar) findViewById(R.id.progressBar3);

        editAngle = (EditText) findViewById(R.id.editAngles);

        Button referButton = (Button) findViewById(R.id.button_reference);
        referButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        mCamera.autoFocus(myAutoFocusCallback);
//                        refColor = bitmap.getPixel(5, 5);
//                        text.append(String.format("#%06X", refColor));
                        picTaken = 0;
                        toTake = 1;
                        mode = 2;
                        mCamera.takePicture(null, null, mPicture);
                    }
                }
        );

        // Add a listener to the Capture button
        Button captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        picTaken = 0;
                        toTake = Integer.parseInt(editAngle.getText().toString());
                        rawData = new int[toTake][mCamera.getParameters().getPictureSize().height];
                        mode = 1;
                        mCamera.takePicture(null, null, mPicture);
                    }
                }
        );

        Button stopButton = (Button) findViewById(R.id.button_stop);
        stopButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        toTake = picTaken;
                    }
                }
        );


        Boolean hasCamera = checkCameraHardware(this.getApplicationContext());

        text.append("hasCamera: " + hasCamera + "\n");

        // Create an instance of Camera
        mCamera = getCameraInstance();

        //resoultion select
        Camera.Parameters params = mCamera.getParameters();

        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            // Autofocus mode is supported
            // set the focus mode
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            // set Camera parameters
            mCamera.setParameters(params);
            text.append("Camera: autofocus enabled\n");
        }

        sizes = params.getSupportedPictureSizes();

        String[] resolutions = new String[sizes.size()];

        int i = 0;
        for (Camera.Size size : sizes) {
            resolutions[i] = size.width + "x" + size.height;
            i++;
        }

        resolutionView = (Spinner) this.findViewById(R.id.spinner);
        ArrayAdapter<String> itemsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, resolutions);
        resolutionView.setAdapter(itemsAdapter);

        resolutionView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here
                Camera.Parameters params = mCamera.getParameters();
                params.setPictureSize(sizes.get(position).width, sizes.get(position).height);
                mCamera.setParameters(params);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });


        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);


    }

    // this is the autofocus call back
    private Camera.AutoFocusCallback myAutoFocusCallback = new Camera.AutoFocusCallback() {

        public void onAutoFocus(boolean autoFocusSuccess, Camera arg1) {
            //Wait.oneSec();
            text.append("focus\n");
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;

        try {
            c = Camera.open(); // attempt to get a Camera instance

        }
        catch (Exception e){
            e.printStackTrace();

            // Camera is not available (in use or does not exist)
            text.append(e.getMessage());
        }
        return c; // returns null if camera is unavailable
    }

    // our handler
    Handler processHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // get the bundle and extract data by key
            Bundle b = msg.getData();
            float elapsed = b.getFloat("elapsed");

            int max = progressbar.getMax();

            //Log.d("progress", "ela" + elapsed);

            progressbar.setProgress((int)(max * elapsed));

            if (b.getBoolean("processed")){

                picTaken++;

                text.setText("pic#: " + picTaken + "\nsize: " +b.getInt("size")/1024 + "kB\nprocessing took " + b.getFloat("took") + "\n" + "center: " + center + "\n");

                capture.setImageBitmap(processed);

                if (picTaken < toTake) {
                    mCamera.takePicture(null, null, mPicture);
                } else {

                    saveXYZ();

                }

            }

        }
    };

    private void processImage(byte[] data){

        final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
        tg.startTone(ToneGenerator.TONE_PROP_BEEP);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;

        bitmap = BitmapFactory.decodeStream(inputStream, null, options);

        //Bitmap BWbitmap = bitmap.

        //text.setText("image size: " + bitmap.getWidth() + "x" + bitmap.getHeight() + "\n");

        int h = bitmap.getHeight();
        int w = bitmap.getWidth();

        int [] pixels = new int[w * h];

        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);

        switch (mode){
            case 1:
                processData(w, h, pixels);
                break;
            case 2:
                center = findCenter(w, h, pixels);
                break;
        }


    }

    private int findCenter(int w, int h, int[] pixels){

        int intensMax = 0;
        int maxPos = 0;
        for (int x=0; x < w; x++) {

            int intens = intensity(pixels[w * (h / 2) + x]);

            if ((intens > intensMax)) {
                intensMax = intens;
                maxPos = x;
            }
        }

        return maxPos;
    }

    private void processData(int w, int h, int[] pixels){

        long start = System.nanoTime();

        for (int y=0; y < h; y++){
            int intensMax = 0;
            for (int x=0; x < w; x++){

                int intens = intensity(pixels[w*y + x]);

                if ( (intens > intensMax ) ) {
                    rawData[picTaken][y] = x;
                    intensMax = intens;
                }

            }

            Message msg = new Message();
            Bundle b = new Bundle();
            b.putFloat("elapsed", (float) (y + 1) / h);
            msg.setData(b);
            // send message to the handler with the current message handler
            processHandler.sendMessage(msg);

        }


        float elapsedTime = System.nanoTime() - start;
        float base = 1000000000;

        //mCamera.takePicture(null, null, mPicture);

        Message msg = new Message();
        Bundle b = new Bundle();
        b.putFloat("elapsed", 1);
        b.putBoolean("processed", true);
        b.putFloat("took", elapsedTime/base);
        msg.setData(b);
        // send message to the handler with the current message handler
        processHandler.sendMessage(msg);
    }

    private void showPreview(){
        /*
        bitmap.eraseColor(Color.WHITE);

        int center = bitmap.getWidth()/2;

        for (int y=0; y<bitmap.getHeight(); y++){

            for (int x=rawData[0][y]; x< w && x < rawData[0][y] + 50; x++){
                bitmap.setPixel(x, y, Color.RED);
            }
        }

        processed = bitmap;
*/
    }

    private void saveXYZ() {

        FileOutputStream outputStream;

        Time now = new Time();
        now.setToNow();

        String filename = "object-" + now.format2445().toString() + ".txt";

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/triD");
        myDir.mkdirs();

        File file = new File(myDir, filename);
        try {
            outputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        int h = bitmap.getHeight();

        float captureCoef = 1/FloatMath.sin((float)(3.14/180)*30);

        for (int img = 0; img < toTake; img++){

            float angle = (float) (img * 2 * 3.14 / 45);
            float sinA = FloatMath.sin(angle);
            float cosA = FloatMath.cos(angle);


            for (int y = 0; y < h; y++) {
                try {
                    outputStream.write((cosA * (center - rawData[img][y]) * captureCoef + " " + (h - y) + " " + sinA * (center - rawData[img][y]) * captureCoef + "\n").getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {

            mCamera.startPreview();

            new Thread(new Runnable() {
                public void run() {
                    processImage(data);
                }
            }).start();

        }
    };


    private int similarToRef(int color1){
        int color2 = refColor;
        /*
        int val = 3*256 - (
                Math.abs( ((color1 & 0x00ff0000) - (color2 & 0x00ff0000)) / 0x10000 ) +
                Math.abs( ((color1 & 0x0000ff00) - (color2 & 0x0000ff00)) / 0x100 ) +
                Math.abs( ((color1 & 0x000000ff) - (color2 & 0x000000ff)) )
        );
        */

        int val = 0xffffffff - (
                ( red(color1) - red(color2) )^2 +
                ( blue(color1) - blue(color2) )^2 +
                ( green(color1) - green(color2) )^2
        );

        //Log.d("comp", "val" + val);
        return val;
    }

    private int intensity(int color1){
//        return (int)(red(color1) * 0.3 + green(color1) * 0.59 + blue(color1) * 0.11);
/*
        for (i = 0; i < pix.length; i++) {
            r = (pix[i]) >> 16 & 0xff;
            g = (pix[i]) >> 8 & 0xff;
            b = (pix[i]) & 0xff;
        }
  */
        return color1 >> 16 & 0xff;
    }



}
