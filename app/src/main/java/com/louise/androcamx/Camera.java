package com.louise.androcamx;

import static android.content.Context.WIFI_SERVICE;

import androidx.activity.OnBackPressedCallback;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Camera extends Fragment {
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private ImageView flashi,flip,picture,video,more,hdr,raw,timedate,flipside,wb,mp;
    private int framesCount;
    private long startTime,elapsedTime;
    private  int ySize,uSize,vSize,quality,width,height;
    private ImageProxy.PlaneProxy[] planes;
    private ImageProxy image;
    private ByteBuffer yBuffer,vBuffer,uBuffer;
    private static int FLASH_THRESHOLD_MIN=80,FLASH_THRESHOLD_MAX=150,flagf=0,FLASH_FRAMES_THRESHOLD=45;
    private byte[] nv21;
    private ByteArrayOutputStream out,tout;
    private YuvImage yuvImage;
    private OutputStream outputStream,outputStream2;
    private Handler handler= new Handler();
    private CameraSelector cameraSelector;
    private CameraControl cameraControl;
    private androidx.camera.core.Camera cam;
    private int camera;
    private int flag=1,flashflag=0,pic=0;
    private SharedPreferences sharedPreferences;
    private Bundle arguments;
    private float avglum=0,fcount=0f,averageLuminance,sum;
    private Boolean camrear=true,moreflag=true,shouldflip=false,recordflag=true,recordflag2=false,rawflag=false,hdrflag=false,mpflag=false;
    private CameraManager cameraManager;
    private ConstraintLayout moreLayout;
    private PrintWriter writer2;
    private MediaCodec mediaCodec;
    private MediaRecorder mediaRecorder;
    private SharedPreferences.Editor imagenum;
    private Surface surface;
    private Bitmap bitmap;
    private Long numlocal;
    private UserViewModel userViewModel;
    private DocumentReference userRef;
    private static final long TIMEOUT_US = 10_000; // Timeout value in microseconds

    private String getnum(){
        numlocal = sharedPreferences.getLong("imagenumber", 0)+1;
        sharedPreferences.edit().putLong("imagenumber", numlocal).apply();
        imagenum.apply();
        return String.format("%08d", numlocal);
    }
    private void decr(){
        long num = sharedPreferences.getLong("imagenumber", 0)-1;
        sharedPreferences.edit().putLong("imagenumber", num).apply();
        imagenum.apply();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_camera, container, false);
        previewView = view.findViewById(R.id.preview_view);
        cameraExecutor = Executors.newSingleThreadExecutor();
        flip=view.findViewById(R.id.flip);
        more=view.findViewById(R.id.more);
        picture=view.findViewById(R.id.picture);
        video=view.findViewById(R.id.video);
        hdr=view.findViewById(R.id.hdr);
        raw=view.findViewById(R.id.raw);
        flipside=view.findViewById(R.id.flipside);
        mp=view.findViewById(R.id.mp);
        mp.setColorFilter(Color.argb(150, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);
        timedate=view.findViewById(R.id.timedate);
        wb=view.findViewById(R.id.wb);
        moreLayout = view.findViewById(R.id.morelayout);
        handleBackButton();
        arguments = getArguments();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        userRef = userViewModel.getUserRef();
// Get the SharedPreferences editor
        imagenum = sharedPreferences.edit();

        if (arguments != null) {
            String[] res=arguments.getString("resolution").split("x");
            height=Integer.parseInt(res[0]);
            width= Integer.parseInt(res[1]);
            ySize = width*height;
            uSize = width*height/2-1;
            vSize = width*height/2-1;
//            System.setProperty("skiagl.skiprenderlog", "false");

            quality=arguments.getInt("quality");
            System.out.println("reso"+":"+width+":"+height+":"+ySize);
            Thread thread = new Thread(() -> {
                try {
                    Socket socket = new Socket(arguments.getString("ip"),arguments.getInt("port"));
                    outputStream = socket.getOutputStream();
                    PrintWriter writer = new PrintWriter(outputStream);
                    String message = "CAMERAINFO:" + arguments.getString("resolution");
                    writer.println(message);
                    writer.flush();
                    handler.post(() -> Toast.makeText(view.getContext(), message, Toast.LENGTH_SHORT).show());

                    Socket socket2 = new Socket(arguments.getString("ip"),arguments.getInt("port"));
                    outputStream2 = socket2.getOutputStream();
                    writer2 = new PrintWriter(outputStream2);
                    String message1 = "SECOND";
                    writer2.println(message1);
                    writer2.flush();
//                    handler.post(() -> Toast.makeText(view.getContext(), message, Toast.LENGTH_SHORT).show());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            thread.start();
            Toast.makeText(view.getContext(), "TCP connection started", Toast.LENGTH_SHORT).show();
        }
        WifiManager wm = (WifiManager) view.getContext().getApplicationContext().getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        Log.d("ip",ip);

        more.setOnClickListener(v->{
            if(moreflag){
                moreLayout.setVisibility(View.GONE);
                more.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.more));
            }else{
                moreLayout.setVisibility(View.VISIBLE);
                more.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.less));
            }
            moreflag=!moreflag;
        });
        more.performClick();
        raw.setOnClickListener(v->{
            if(rawflag) raw.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.rawoff));
            else raw.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.rawon));
            rawflag=!rawflag;
        });
        hdr.setOnClickListener(v->{
            if(hdrflag) hdr.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.hdroff));
            else hdr.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.hdron));
            hdrflag=!hdrflag;
        });
        rawflag = !sharedPreferences.getBoolean("raw", false);
        raw.performClick();
        flipside.setOnClickListener(v->{
          new Thread(() -> {
              writer2.println("FLIP");
              writer2.flush();
          }).start();
        });
        mp.setOnClickListener(v->{
            new Thread(() -> {
                writer2.println("MEPI");
                writer2.flush();
            }).start();
            mpflag=!mpflag;
            if(mpflag){
                mp.setColorFilter(Color.argb(0, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);
            }
            else{
                mp.setColorFilter(Color.argb(150, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);
            }
        });

        flashflag = sharedPreferences.getBoolean("flash", false)?(sharedPreferences.getBoolean("autoflash", false)?2:1):0;
        flashi=view.findViewById(R.id.flash);
        flashlogic();
//        Toast.makeText(getContext(), "flash "+flashflag, Toast.LENGTH_SHORT).show();
        flashi.setOnClickListener(v ->{
            if(flashflag==0){
                flashflag=3;
                cameraControl.enableTorch(true);
//                val meteringPoint1 = meteringPointFactory.createPoint(x1, x1)
//                val meteringPoint2 = meteringPointFactory.createPoint(x2, y2)
//                val action = FocusMeteringAction.Builder(meteringPoint1) // default AF|AE|AWB
//                        // Optionally add meteringPoint2 for AF/AE.
//                        .addPoint(meteringPoint2, FLAG_AF | FLAG_AE)
//                        // The action is canceled in 3 seconds (if not set, default is 5s).
//                        .setAutoCancelDuration(3, TimeUnit.SECONDS)
//                        .build()
//
//                val result = cameraControl.startFocusAndMetering(action)
                cameraControl.startFocusAndMetering(
                        new FocusMeteringAction.Builder(
                                new SurfaceOrientedMeteringPointFactory(1f, 1f)
                                        .createPoint(.5f, .5f),
                                FocusMeteringAction.FLAG_AF
                        ).setAutoCancelDuration(2, TimeUnit.SECONDS).build());
                Toast.makeText(getContext(), "auto flash", Toast.LENGTH_SHORT).show();


            }else if(flashflag==3 || flashflag==1){
                cameraControl.enableTorch(false);
                flashflag=2;
            }else{
                flashflag=0;
                cameraControl.enableTorch(false);
            }
            flashlogic();
        });
        Log.d("loctaion",getContext().getFilesDir().getAbsolutePath());
        picture.setOnClickListener(v->{
            String parentFolderName = "AndroCamX";
            String childFolderName = "Images";
            File parentFolder = new File(Environment.getExternalStorageDirectory(), parentFolderName);
            File childFolder;
            if (!parentFolder.exists()) {
                if (parentFolder.mkdir()) {
                    childFolder = new File(parentFolder, childFolderName);
                    if (childFolder.mkdir()) {
                        // Child folder created successfully
                    } else {

                        Toast.makeText(getContext(), "failed parent", Toast.LENGTH_SHORT).show();
                        // Failed to create child folder
                    }
                } else {
                    return;
                }
            } else {
                childFolder = new File(parentFolder, childFolderName);
                if (!childFolder.exists()) {
                    if (childFolder.mkdir()) {
                        // Child folder created successfully
                    } else {

                        Toast.makeText(getContext(), "failed child", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    // Child folder already exist
                }
            }
            String imagePath = childFolder.getAbsolutePath()+File.separator +"Image"+getnum()+".jpg";  // Replace "my_image.jpg" with your desired file name
            Log.d("Folder","Images folder created");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // Save the image as JPEG
//            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), quality, outputStream);
//            byte[] jpegData = outputStream.toByteArray();
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), rawflag?100:quality, outputStream);

            byte[] jpegData= outputStream.toByteArray();
            // Save the image as PNG
//            byte[] imageBytes=tout.toByteArray();
//            System.out.println(imageBytes.length+" "+yuvImage.getWidth()*yuvImage.getHeight()+" ");
//            Bitmap pngBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, uSize+vSize+ySize);
//            pngBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(imageBytes));
//            pngBitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
//            byte[] pngData = outputStream.toByteArray();
//
//            // Save the image as WebP (lossless)
//            Bitmap webpBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
//            webpBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(imageBytes));
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
////                webpBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 0, outputStream);
//            }
//            byte[] webpData = outputStream.toByteArray();
            try {
//                FileOutputStream os = new FileOutputStream(imagePath);
//                FileOutputStream jpegOutputStream = new FileOutputStream(childFolder.getAbsolutePath()+File.separator + "my_image.jpg");
//                jpegOutputStream.write(jpegData);
//                jpegOutputStream.close();
                FileOutputStream jpegOutputStream = new FileOutputStream(imagePath);
                jpegOutputStream.write(jpegData);
                jpegOutputStream.close();
                userRef.update("ImageNumber", String.valueOf(numlocal));
                // Save PNG image
//                FileOutputStream pngOutputStream = new FileOutputStream(childFolder.getAbsolutePath()+File.separator + "my_image.png");
//                pngOutputStream.write(pngData);
//                pngOutputStream.close();
//
//                // Save WebP (lossless) image
//                FileOutputStream webpOutputStream = new FileOutputStream(childFolder.getAbsolutePath()+File.separator + "my_image.webp");
//                webpOutputStream.write(webpData);
//                webpOutputStream.close();
//                os.write(out.toByteArray());
//                os.close();
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, os);
//                }

                // Flush and close the output stream
                jpegOutputStream.flush();
                jpegOutputStream.close();
                // Image saved successfully
                Toast.makeText(getContext(),"Image Saved : "+imagePath, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                // Error occurred while saving the image
                Log.e("SaveImage", "Error saving image5: " + e.getMessage());
                decr();
            }
        });
        Toast.makeText(getContext(), "Camera :"+arguments.getString("camera"), Toast.LENGTH_SHORT).show();
        camrear=arguments.getString("camera").startsWith("Back")?false:true;
        flip.setOnClickListener(v->{
                camrear=!camrear;
                camicon();
                if(camrear)cameraSelector= CameraSelector.DEFAULT_BACK_CAMERA;
                else cameraSelector= CameraSelector.DEFAULT_FRONT_CAMERA;
            ProcessCameraProvider.getInstance(view.getContext()).addListener(()-> {
                    try {
                        cameraListner(view);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, ContextCompat.getMainExecutor(requireContext()));
        });
        flip.performClick();
        video.setOnClickListener(v->{
            if(recordflag) {
                recordflag2=true;
                initializeMediaRecorder(width, height);
            }
            else {
                recordflag2=false;
                stopRecording();
            }
            recordflag=!recordflag;
        });
        return view;
    }
    public void cameraListner(View view) throws CameraAccessException, ExecutionException, InterruptedException {
        Matrix matrix = new Matrix();
        matrix.setScale(-1, 1);
        out=new ByteArrayOutputStream();
        tout=new ByteArrayOutputStream();
        ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(getContext()).get();
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        previewView.setScaleX(-1);
//        preview.setTargetRotation(Surface.ROTATION_180);
//        ImageView imageView = new ImageView(view.getContext());
//        view.setRotationX(180);
//        view.setRotationY(180);
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        ImageAnalysis imageAnalysis = builder
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(new Size(width,height))
//                .setMirrorMode(true)
                .build();
        startTime = SystemClock.elapsedRealtime();
        framesCount = 0;
        AtomicLong frameIndex = new AtomicLong();
        long frameRate = 30; // Replace with your desired frame rate

// Inside your frame proce
        nv21 = new byte[ySize + uSize + vSize];
        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
            this.image=image;
            System.out.println(image.getHeight()+" "+image.getWidth());
            planes = image.getPlanes();
//            yBuffer = planes[0].getBuffer();
//            yBuffer.rewind();
//            if(flashflag==2){
//                sum = 0;
//                for (int i = 0; i < ySize; i++) {
//                    sum += yBuffer.get() & 0xFF;
//                }
//                yBuffer.rewind();
//                averageLuminance = sum / ySize;
//                fcount += 1;
//                avglum += averageLuminance;
//                if (fcount >= 60) {
//                    avglum /= fcount;
//                    fcount = 0;
//                }
//                if (averageLuminance < FLASH_THRESHOLD_MIN) {
//                    if (flagf == 0 && avglum / fcount < FLASH_THRESHOLD_MIN && fcount > FLASH_FRAMES_THRESHOLD) {
//                        fcount = avglum = 0;
//                        flagf = 1;
//                        cameraControl.enableTorch(true);
//                    }
//                } else if (averageLuminance > FLASH_THRESHOLD_MAX) {
//                    avglum += averageLuminance;
//                    if (flagf == 1 && avglum / fcount > FLASH_THRESHOLD_MAX && fcount > FLASH_FRAMES_THRESHOLD) {
//                        flagf = 0;
//                        fcount = avglum = 0;
//                        cameraControl.enableTorch(false);
//                    }
//                }
//                Log.d("lum",fcount+" "+avglum+" "+averageLuminance);
//            }
//            uBuffer = planes[1].getBuffer();
//            uBuffer.rewind();
//            vBuffer = planes[2].getBuffer();
//            vBuffer.rewind();
//            nv21 = new byte[ySize + uSize + vSize];
//            yBuffer.get(nv21, 0, ySize);
////            vBuffer.get(nv21, ySize, vSize);
////            uBuffer.get(nv21, ySize + vSize, uSize);
//
//            vBuffer.get(nv21, ySize, vSize);
//            uBuffer.get(nv21, ySize + vSize, uSize);
            nv21= yuv420ThreePlanesToNV21(planes, image.getWidth(), image.getHeight());
            yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
//            bitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
//            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(nv21));
            out.reset();
            tout.reset();
            long presentationTimeUs = (long) (frameIndex.get() * (1_000_000.0 / frameRate));

            // Process the frame data using the presentation timestamp
            if(recordflag2) {
                processFrameData(out, presentationTimeUs);
                frameIndex.getAndIncrement();
            }
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), quality, out);
//            if (shouldflip) {
//                Bitmap flippedBitmap = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size());
//                Bitmap flippedImage = Bitmap.createBitmap(flippedBitmap, 0, 0, flippedBitmap.getWidth(), flippedBitmap.getHeight(), matrix, true);
//                ByteArrayOutputStream flippedOut = new ByteArrayOutputStream();
//                flippedImage.compress(Bitmap.CompressFormat.JPEG, quality, flippedOut);
//                out = flippedOut;
                // Display the flipped image
//                Bitmap flippedImageBitmap = BitmapFactory.decodeByteArray(flippedOut.toByteArray(), 0, flippedOut.size());
//
//                imageView.setImageBitmap(flippedImageBitmap);
//            }

            elapsedTime = (SystemClock.elapsedRealtime() - startTime);
            framesCount++;
            if (elapsedTime >= 1000) {
                float fps = framesCount / (elapsedTime / 1000f);
                Log.d("fps", "FPS: " + fps);
                framesCount = 0;
                startTime = SystemClock.elapsedRealtime();
            }
            if (flag==1){
                try {
                    System.out.println(out.size());
                    outputStream.write(ByteBuffer.allocate(4).putInt(out.size()).array());
                    outputStream.flush();
                    outputStream.write(out.toByteArray());
                    outputStream.flush();
                } catch (IOException e) {
                    Log.e("error", String.valueOf(e));
                }
            }
            image.close();
        });
        cameraManager = (CameraManager) view.getContext().getSystemService(Context.CAMERA_SERVICE);
        String cameraId = cameraManager.getCameraIdList()[camera];
        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
        boolean isFlashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        Toast.makeText(getContext(), String.valueOf(isFlashAvailable), Toast.LENGTH_SHORT).show();
        cameraProvider.unbindAll();
        cam = cameraProvider.bindToLifecycle(getActivity(), cameraSelector, preview, imageAnalysis);
        cameraControl = cam.getCameraControl();
        if ( cam.getCameraInfo().hasFlashUnit() ) {
        }
    }
    public void flashlogic(){
        if(flashflag==0){flashi.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.flashoff));}
        else if(flashflag==2){flashi.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.flashauto));}
        else{flashi.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.flashon));}
    }
    public void camicon(){
        if(camrear){
            flip.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.rear));
            flashi.setClickable(true);
            flashi.setColorFilter(Color.argb(0, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);}
        else{
            flip.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.front));
            flashi.setClickable(false);
            flashflag=0;
            flashlogic();
            flashi.setColorFilter(Color.argb(150, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);
        }
    }
    private void initializeMediaRecorder(int width, int height) {
        try {
            Log.d("Video","starting");
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setVideoSize(width, height);
            String dcimFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
            String folderName = "AndroCamX_Videos";  // Replace with your desired folder name
            String videoPath = dcimFolderPath + File.separator + folderName + File.separator + "my_vid.mp4";  // Replace "my_image.jpg" with your desired file name

            File folder = new File(dcimFolderPath, folderName);
            if (!folder.exists()) {
                if (!folder.mkdirs()) {
                    // Failed to create the directory
                    Log.e("SaveImage", "Error creating directory: " + folder.getAbsolutePath());
                    return;
                }
            }
//            String videoPath = "/path/to/save/video.mp4"; // Replace with your desired file path
            mediaRecorder.setOutputFile(videoPath);
            mediaRecorder.prepare();
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 2000000); // Default video bit rate
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30); // Default frame rate
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2); // Default I-frame interval
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            surface = mediaCodec.createInputSurface();
            mediaCodec.start();
            mediaRecorder.setInputSurface(surface);
            mediaRecorder.start();
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
            // Handle initialization errors
        }
    }

    private void processFrameData(ByteArrayOutputStream jpegData, long presentationTimeUs) {
        try {Log.d("Video","processing");
            byte[] frameData = jpegData.toByteArray();

            int inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_US);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                inputBuffer.put(frameData);

                mediaCodec.queueInputBuffer(inputBufferIndex, 0, frameData.length, presentationTimeUs, 0);
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
            if (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                mediaRecorder.setOutputFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/my_video.mp4");
//                mediaRecorder.writeSampleData(outputBufferIndex, outputBuffer, bufferInfo);
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            // Handle frame processing errors
        }
    }

    private void stopRecording() {
        try {Log.d("Video","stoping");
            mediaCodec.stop();
            mediaCodec.release();
            mediaRecorder.stop();
            mediaRecorder.release();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            // Handle stop recording errors
        }
    }
    private void handleBackButton() {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Pop the fragment from the back stack to go back to the previous fragment
                requireActivity().getSupportFragmentManager().popBackStack();

                // If you want to let the parent activity handle the back button press
                // and perform the default behavior, simply remove the popBackStack() call
                // super.handleOnBackPressed();
            }
        });
    }
    public String getTitle() {
        return "About";
    }
    static byte[] yuv420ThreePlanesToNV21(
            ImageProxy.PlaneProxy[] yuv420888planes, int width, int height) {
        int imageSize = width * height;
        byte[] out = new byte[imageSize + 2 * (imageSize / 4)];

        if (areUVPlanesNV21(yuv420888planes, width, height)) {
            yuv420888planes[0].getBuffer().get(out, 0, imageSize);
            ByteBuffer uBuffer = yuv420888planes[1].getBuffer();
            ByteBuffer vBuffer = yuv420888planes[2].getBuffer();
            vBuffer.get(out, imageSize, 1);
            uBuffer.get(out, imageSize + 1, 2 * imageSize / 4 - 1);
        } else {
            unpackPlane(yuv420888planes[0], width, height, out, 0, 1);
            unpackPlane(yuv420888planes[1], width, height, out, imageSize + 1, 2);
            unpackPlane(yuv420888planes[2], width, height, out, imageSize, 2);
        }

        return out;
    }
    private static boolean areUVPlanesNV21(ImageProxy.PlaneProxy[] planes, int width, int height) {
        int imageSize = width * height;

        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        // Backup buffer properties.
        int vBufferPosition = vBuffer.position();
        int uBufferLimit = uBuffer.limit();

        // Advance the V buffer by 1 byte, since the U buffer will not contain the first V value.
        vBuffer.position(vBufferPosition + 1);
        // Chop off the last byte of the U buffer, since the V buffer will not contain the last U value.
        uBuffer.limit(uBufferLimit - 1);

        // Check that the buffers are equal and have the expected number of elements.
        boolean areNV21 =
                (vBuffer.remaining() == (2 * imageSize / 4 - 2)) && (vBuffer.compareTo(uBuffer) == 0);

        // Restore buffers to their initial state.
        vBuffer.position(vBufferPosition);
        uBuffer.limit(uBufferLimit);

        return areNV21;
    }
    private static void unpackPlane(
            ImageProxy.PlaneProxy plane, int width, int height, byte[] out, int offset, int pixelStride) {
        ByteBuffer buffer = plane.getBuffer();
        buffer.rewind();

        // Compute the size of the current plane.
        // We assume that it has the aspect ratio as the original image.
        int numRow = (buffer.limit() + plane.getRowStride() - 1) / plane.getRowStride();
        if (numRow == 0) {
            return;
        }
        int scaleFactor = height / numRow;
        int numCol = width / scaleFactor;

        // Extract the data in the output buffer.
        int outputPos = offset;
        int rowStart = 0;
        for (int row = 0; row < numRow; row++) {
            int inputPos = rowStart;
            for (int col = 0; col < numCol; col++) {
                out[outputPos] = buffer.get(inputPos);
                outputPos += pixelStride;
                inputPos += plane.getPixelStride();
            }
            rowStart += plane.getRowStride();
        }
    }

}