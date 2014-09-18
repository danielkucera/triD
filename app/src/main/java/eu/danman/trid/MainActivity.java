package eu.danman.trid;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;


public class MainActivity extends ActionBarActivity {

    static TextView text;

    private Camera mCamera;
    private CameraPreview mPreview;
    private ImageView capture;
    private int refColor = 0xffff0000;
    private Spinner resolutionView;
    private String[] resolutions;
    List<Camera.Size> sizes;
    Bitmap processed;
    ProgressBar progressbar;
    int picTaken;
    int toTake;
    EditText editAngle;
    int mode;

    List<Integer[]> rawData;
    List<List<triPoint>> pointData;
    int width;
    int angle;

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

        drawOverlay();

        Button focusButton = (Button) findViewById(R.id.button_focus);
        focusButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        mCamera.autoFocus(myAutoFocusCallback);
                    }
                }
        );


        Button saveTRIButton = (Button) findViewById(R.id.button_saveTRI);
        saveTRIButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        saveTRI();
                    }
                }
        );

        Button loadTRIButton = (Button) findViewById(R.id.button_loadTRI);
        loadTRIButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadTRI(new File("/sdcard/triD/","object-last.tri"));
                    }
                }
        );


        Button saveXYZButton = (Button) findViewById(R.id.button_saveXYZ);
        saveXYZButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        saveXYZ();
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
                        rawData = new ArrayList<Integer[]>();
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

        Button medianButton = (Button) findViewById(R.id.button_median);
        medianButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        filterMedian2(5);
                    }
                }
        );

        Button saveSTL = (Button) findViewById(R.id.button_saveSTL);
        saveSTL.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        saveSTL();
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

    private void drawOverlay(){
        //TODO: spravne rozmery
//        int h = iv.getMeasuredHeight();
//        int w = iv.getMeasuredWidth();
        int h = 240;
        int w = 320;

        ImageView previewOverlay = (ImageView) findViewById(R.id.imageView2);

        Log.d("bitmap overlay", "width " + w + " height " + h);

        Bitmap overlay = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        for (int i=0; i< h; i++){
            overlay.setPixel((int)(w / 2),i, Color.WHITE);
        }

        previewOverlay.setImageBitmap(overlay);

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

                text.setText("pic#: " + picTaken + "\nsize: " +b.getInt("size")/1024 + "kB\nprocessing took " + b.getFloat("took") + "\n" );

                capture.setImageBitmap(processed);

                if (picTaken < toTake) {
                    mCamera.takePicture(null, null, mPicture);
                } else {

                    ///saveXYZ();

                }

            }

        }
    };

    private void processImage(byte[] data){

        Bitmap bitmap;

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

        Log.d("bitmap", "size is: "+ data.length);

        width = w;

        int [] pixels = new int[w * h];

        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);

        switch (mode){
            case 1:
                processData(w, h, pixels);
                break;
            case 2:
//                center = findCenter(w, h, pixels);
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

        Log.d("pixels", "value: "+ pixels[(int)(3264*1224.5)]);

        long start = System.nanoTime();

        Integer rawFrame[];

        rawFrame = new Integer[h];

        for (int y=0; y < h; y++) {
            int intensMax = 0;
            for (int x = 0; x < w; x++) {

                int intens = intensity(pixels[w * y + x]);

                if ((intens > intensMax)) {
                    rawFrame[y] = x;
                    intensMax = intens;
                    if (intens > 254) {
                        x = w;
                    }
                }

            }
        }

        rawData.add(rawFrame);

        Log.d("rawFrame", "center val: "+ rawFrame[1224]);
        Log.d("rawData", "len: " + rawData.size());

        float elapsedTime = System.nanoTime() - start;
        float base = 1000000000;

        Message msg = new Message();
        Bundle b = new Bundle();
        b.putFloat("elapsed", 1);
        b.putBoolean("processed", true);
        b.putFloat("took", elapsedTime/base);
        msg.setData(b);
        // send message to the handler with the current message handler
        processHandler.sendMessage(msg);
    }

    void generatePoints(){

        pointData = new ArrayList<List<triPoint>>(rawData.size());

        float captureCoef = 1/FloatMath.sin((float)(3.14/180)*30);  //TODO: uhol

        int h = rawData.get(0).length;
        int center = width/2;
        int frames = rawData.size();
        Integer rawFrame[];

        Log.d("generate points", "h: " + h + " frames: " + frames);


        for (int frame = 0; frame < frames; frame++){

            List<triPoint> pointFrame = new ArrayList<triPoint>(h);

            rawFrame = rawData.get(frame);

            float angle = (float) (frame * 2 * 3.14 / frames);
            float sinA = FloatMath.sin(angle);
            float cosA = FloatMath.cos(angle);

            for (int y = 0; y < h; y++) {
                double xP = cosA * (center - rawData.get(frame)[y]) * captureCoef;
                double yP = (h - y);
                double zP =  sinA * (center - rawData.get(frame)[y]) * captureCoef;
                triPoint point = new triPoint(xP, yP, zP);

                pointFrame.add(point);
            }

            pointData.add(pointFrame);

        }


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

        generatePoints();

        BufferedOutputStream outputStream;

        Time now = new Time();
        now.setToNow();

        String filename = "object-" + now.format2445().toString() + ".xyz";

        String root = Environment.getExternalStorageDirectory().toString();
//        String root = "/storage";
        File myDir = new File(root + "/triD");
        myDir.mkdirs();

        File file = new File(myDir, filename);
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        Log.d("save xyz", "start");


        for (List<triPoint> pointFrame : pointData){

            for (triPoint point : pointFrame){
                try {
                    outputStream.write((point.getX() + " " + point.getY() + " " + point.getZ() + "\n").getBytes());
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

        MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, null);

        Toast.makeText(this, "file " + filename + " saved!", Toast.LENGTH_SHORT).show();

    }

    private void saveSTL() {

        BufferedOutputStream outputStream;

        generatePoints();

        Time now = new Time();
        now.setToNow();

        String filename = "object-" + now.format2445().toString() + ".stl";

        String root = Environment.getExternalStorageDirectory().toString();
//        String root = "/storage";
        File myDir = new File(root + "/triD");
        myDir.mkdirs();

        File file = new File(myDir, filename);
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }


        Log.d("save stl", " frames: " );

        try {

            outputStream.write(("solid object\n").getBytes());

            for (int frame = 0; frame < pointData.size() - 1; frame++){

                List<triPoint> pointFrame = pointData.get(frame);
                List<triPoint> pointFrame2 = pointData.get(frame+1);

                for (int y = 0; y < pointFrame.size() - 1; y++) {

                    triPoint a = pointFrame.get(y);
                    triPoint b = pointFrame2.get(y);
                    triPoint c = pointFrame.get(y + 1);
                    triPoint d = pointFrame2.get(y + 1);

                    // A B
                    // C D

                    outputStream.write(("facet normal 0 0 0\n").getBytes());
                    outputStream.write(("outer loop\n").getBytes());

                    outputStream.write(("vertex " + a.getX() + " " + a.getY() + " " + a.getZ() + "\n").getBytes());
                    outputStream.write(("vertex " + b.getX() + " " + b.getY() + " " + b.getZ() + "\n").getBytes());
                    outputStream.write(("vertex " + c.getX() + " " + c.getY() + " " + c.getZ() + "\n").getBytes());

                    outputStream.write(("endloop\n").getBytes());
                    outputStream.write(("endfacet\n").getBytes());

                    //<
                    outputStream.write(("facet normal 0 0 0\n").getBytes());
                    outputStream.write(("outer loop\n").getBytes());


                    outputStream.write(("vertex " + b.getX() + " " + b.getY() + " " + b.getZ() + "\n").getBytes());
                    outputStream.write(("vertex " + c.getX() + " " + c.getY() + " " + c.getZ() + "\n").getBytes());
                    outputStream.write(("vertex " + d.getX() + " " + d.getY() + " " + d.getZ() + "\n").getBytes());

                    outputStream.write(("endloop\n").getBytes());
                    outputStream.write(("endfacet\n").getBytes());

                }

            }

            outputStream.write(("endsolid object\n").getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, null);

        Toast.makeText(this, "file " + filename + " saved!", Toast.LENGTH_SHORT).show();

    }

    private void saveTRI() {

        FileOutputStream outputStream;
        ByteBuffer b = ByteBuffer.allocate(4);

        Time now = new Time();
        now.setToNow();

//        String filename = "object-" + now.format2445().toString() + ".tri";
        String filename = "object-last.tri";

        String root = Environment.getExternalStorageDirectory().toString();
//        String root = "/storage";
        File myDir = new File(root + "/triD");
        myDir.mkdirs();

        File file = new File(myDir, filename);
        try {
            outputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        float captureCoef = 1/FloatMath.sin((float)(3.14/180)*30);  //TODO: uhol

        int h = rawData.get(0).length;
        int center = width/2;
        int frames = rawData.size();
        Integer rawFrame[];

        Log.d("save xyz", "h: " + h + " frames: " + frames);


        try {
            b.clear();
            b.putInt(width);
            outputStream.write(b.array());

            b.clear();
            b.putInt(h);
            outputStream.write(b.array());

            b.clear();
            b.putInt(frames);
            outputStream.write(b.array());

        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int frame = 0; frame < frames; frame++){

            rawFrame = rawData.get(frame);

            for (int y = 0; y < h; y++) {
                b.clear();
                b.putInt(rawFrame[y].intValue());

                try {
                    outputStream.write(b.array());
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

        MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, null);

        Toast.makeText(this, "file " + filename + " saved!", Toast.LENGTH_SHORT).show();

    }

    private void loadTRI(File file) {

        FileInputStream inputStream;
        ByteBuffer b = ByteBuffer.allocate(4);
        byte bArr[] = new byte[4];

        rawData = new ArrayList<Integer[]>();

        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        try {
            inputStream.read(bArr, 0, 4);
            width = ByteBuffer.wrap(bArr).getInt();

            inputStream.read(bArr, 0, 4);
            int h = ByteBuffer.wrap(bArr).getInt();

            inputStream.read(bArr, 0, 4);
            int frames = ByteBuffer.wrap(bArr).getInt();


            for (int frame=0; frame < frames; frame++){
                Integer rawFrame[] = new Integer[h];

                for (int y=0; y < h; y++){
                    inputStream.read(bArr, 0, 4);
                    rawFrame[y] = ByteBuffer.wrap(bArr).getInt();
                }

                rawData.add(rawFrame);

            }
            Log.d("loaded", "h: " + h + " frames: " + frames);

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Toast.makeText(this, "file loaded!", Toast.LENGTH_SHORT).show();

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

    private void filterMedian(int grade){
        List<Integer[]> newRawData = new ArrayList<Integer[]>();
        Integer[] newRawFrame;
        Integer[] sortBuff = new Integer[grade];
        int median = grade/2 + 1;

        Log.d("filter","median: " + median);

        for (Integer[] rawFrame : rawData){

            newRawFrame = new Integer[rawFrame.length-grade];

            for(int y=0; y < rawFrame.length - grade; y++){

                for(int x=0; x < grade; x++){
                    sortBuff[x] = rawFrame[x+y];
                }

                Arrays.sort(sortBuff);

                newRawFrame[y] = sortBuff[median];
            }

            newRawData.add(newRawFrame);
        }

        rawData = newRawData;

    }

    private void filterMedian2(int grade){
        List<Integer[]> newRawData = new ArrayList<Integer[]>();
        Integer[] newRawFrame;
        Integer[] sortBuff = new Integer[grade];
        int median = grade/2;

        Log.d("filter","median2: " + median);

        for (Integer[] rawFrame : rawData){

            newRawFrame = new Integer[rawFrame.length/(median)];

            for(int y=0; y < rawFrame.length - grade; y+= median){

                for(int x=0; x < grade; x++){
                    sortBuff[x] = rawFrame[x+y];
                }

                Arrays.sort(sortBuff);

                newRawFrame[y] = sortBuff[median];
            }

            newRawData.add(newRawFrame);
        }

        rawData = newRawData;

        Log.d("filter","median2 end");

    }


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
