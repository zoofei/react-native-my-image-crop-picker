package com.mg.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.yalantis.ucrop.UCropActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import cn.finalteam.rxgalleryfinal.RxGalleryFinal;
import cn.finalteam.rxgalleryfinal.RxGalleryFinalApi;
import cn.finalteam.rxgalleryfinal.bean.ImageCropBean;
import cn.finalteam.rxgalleryfinal.bean.MediaBean;
import cn.finalteam.rxgalleryfinal.imageloader.ImageLoaderType;
import cn.finalteam.rxgalleryfinal.rxbus.RxBusResultDisposable;
import cn.finalteam.rxgalleryfinal.rxbus.event.ImageMultipleResultEvent;
import cn.finalteam.rxgalleryfinal.rxbus.event.ImageRadioResultEvent;
import cn.finalteam.rxgalleryfinal.ui.RxGalleryListener;
import cn.finalteam.rxgalleryfinal.ui.base.IRadioImageCheckedListener;

class PickerModule extends ReactContextBaseJavaModule  {
    private static final int IMAGE_PICKER_REQUEST = 61110;
    private static final int CAMERA_PICKER_REQUEST = 61111;
    private static final String E_ACTIVITY_DOES_NOT_EXIST = "E_ACTIVITY_DOES_NOT_EXIST";

    private static final String E_PICKER_CANCELLED_KEY = "E_PICKER_CANCELLED";
    private static final String E_PICKER_CANCELLED_MSG = "User cancelled image selection";

    private static final String E_CALLBACK_ERROR = "E_CALLBACK_ERROR";
    private static final String E_FAILED_TO_SHOW_PICKER = "E_FAILED_TO_SHOW_PICKER";
    private static final String E_FAILED_TO_OPEN_CAMERA = "E_FAILED_TO_OPEN_CAMERA";
    private static final String E_NO_IMAGE_DATA_FOUND = "E_NO_IMAGE_DATA_FOUND";
    private static final String E_CAMERA_IS_NOT_AVAILABLE = "E_CAMERA_IS_NOT_AVAILABLE";
    private static final String E_CANNOT_LAUNCH_CAMERA = "E_CANNOT_LAUNCH_CAMERA";
    private static final String E_PERMISSIONS_MISSING = "E_PERMISSION_MISSING";
    private static final String E_ERROR_WHILE_CLEANING_FILES = "E_ERROR_WHILE_CLEANING_FILES";

    private Promise mPickerPromise;

    private String mediaType = "photo";
    private boolean multiple = false;
    private boolean includeBase64 = false;
    private boolean cropping = false;
    private boolean cropperCircleOverlay = false;
    private boolean showCropGuidelines = true;
    private boolean hideBottomControls = false;
    private boolean enableRotationGesture = false;
    private ReadableMap options;

    //Grey 800
    private final String DEFAULT_TINT = "#424242";
    private String cropperActiveWidgetColor = DEFAULT_TINT;
    private String cropperStatusBarColor = DEFAULT_TINT;
    private String cropperToolbarColor = DEFAULT_TINT;

    //Light Blue 500
    private int width = 200;
    private int height = 200;

    private int minFiles = 1;
    private int maxFiles = 5;

    private int compressQuality = -1;
    private final ReactApplicationContext mReactContext;

    private ResultCollector resultCollector;
    private Compression compression = new Compression();



    PickerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;
        RxGalleryListener
                .getInstance()
                .setRadioImageCheckedListener(
                        new IRadioImageCheckedListener() {
                            @Override
                            public void cropAfter(Object t) {
                                Toast.makeText(mReactContext, t.toString(), Toast.LENGTH_SHORT).show();
                                if(cropping) {
                                    try {
                                        WritableArray resultArr = new WritableNativeArray();
                                        resultArr.pushMap(getImage(mReactContext.getCurrentActivity(), t.toString()));
                                        mPickerPromise.resolve(resultArr);
                                    }catch(Exception e){}
                                }
                            }

                            @Override
                            public boolean isActivityFinish() {
                                return true;
                            }
                        });

    }

    @Override
    public String getName() {
        return "ImageCropPicker";
    }

    private void setConfiguration(final ReadableMap options) {
        mediaType = options.hasKey("mediaType") ? options.getString("mediaType") : mediaType;
        multiple = options.hasKey("multiple") && options.getBoolean("multiple");
        includeBase64 = options.hasKey("includeBase64") && options.getBoolean("includeBase64");
        width = options.hasKey("width") ? options.getInt("width") : width;
        height = options.hasKey("height") ? options.getInt("height") : height;
        cropping = options.hasKey("cropping") ? options.getBoolean("cropping") : cropping;
        cropperActiveWidgetColor = options.hasKey("cropperActiveWidgetColor") ? options.getString("cropperActiveWidgetColor") : cropperActiveWidgetColor;
        cropperStatusBarColor = options.hasKey("cropperStatusBarColor") ? options.getString("cropperStatusBarColor") : cropperStatusBarColor;
        cropperToolbarColor = options.hasKey("cropperToolbarColor") ? options.getString("cropperToolbarColor") : cropperToolbarColor;
        cropperCircleOverlay = options.hasKey("cropperCircleOverlay") ? options.getBoolean("cropperCircleOverlay") : cropperCircleOverlay;
        showCropGuidelines = options.hasKey("showCropGuidelines") ? options.getBoolean("showCropGuidelines") : showCropGuidelines;
        hideBottomControls = options.hasKey("hideBottomControls") ? options.getBoolean("hideBottomControls") : hideBottomControls;
        enableRotationGesture = options.hasKey("enableRotationGesture") ? options.getBoolean("enableRotationGesture") : enableRotationGesture;

        RxGalleryFinalApi.setImgSaveRxSDCard("SmartWork");
        RxGalleryFinalApi.setImgSaveRxCropSDCard("SmartWork/crop");
        this.options = options;
    }

    private WritableMap getImage(final Activity activity,String path) throws Exception {
        WritableMap image = new WritableNativeMap();
        if (path.startsWith("http://") || path.startsWith("https://")) {
            throw new Exception("Cannot select remote files");
        }
        validateImage(path);

        // if compression options are provided image will be compressed. If none options is provided,
        // then original image will be returned
        File compressedImage = compression.compressImage(activity, options, path);
        String compressedImagePath = compressedImage.getPath();
        BitmapFactory.Options options = validateImage(compressedImagePath);

        image.putString("path", "file://" + compressedImagePath);
        image.putInt("width", options.outWidth);
        image.putInt("height", options.outHeight);
        image.putString("mime", options.outMimeType);
        image.putInt("size", (int) new File(compressedImagePath).length());

        if (includeBase64) {
            image.putString("data", getBase64StringFromFile(compressedImagePath));
        }

        return image;
    }

    private WritableMap getImage(final Activity activity, ImageCropBean result) throws Exception {

        String path = result.getOriginalPath();
        return getImage(activity,path);
    }
    private WritableMap getImage(final Activity activity,MediaBean result) throws Exception {

        String path = result.getOriginalPath();
        return getImage(activity,path);
    }


    private void initImageLoader(Activity activity) {

        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(activity);
        config.threadPriority(Thread.NORM_PRIORITY - 2);
        config.denyCacheImageMultipleSizesInMemory();
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        config.diskCacheSize(50 * 1024 * 1024); // 50 MiB
        config.tasksProcessingOrder(QueueProcessingType.LIFO);
        ImageLoader.getInstance().init(config.build());
    }

    @ReactMethod
    public void openPicker(final ReadableMap options, final Promise promise) {
        final Activity activity = getCurrentActivity();

        if (activity == null) {
            promise.reject(E_ACTIVITY_DOES_NOT_EXIST, "Activity doesn't exist");
            return;
        }

        RxGalleryFinal rxGalleryFinal = RxGalleryFinal.with(mReactContext);

        setConfiguration(options);
        initImageLoader(activity);
        mPickerPromise = promise;

        if(mediaType.equals("photo")){
            rxGalleryFinal.image()
            .imageLoader(ImageLoaderType.UNIVERSAL);
        }else{
            cropping = false;
            rxGalleryFinal.video();
        }

        if(!this.multiple) {
            if(cropping){
                rxGalleryFinal.crop()
                        .cropMaxResultSize(this.width,this.height)
                        .cropHideBottomControls(hideBottomControls)
                        .cropropCompressionQuality(100)
                        .cropWithAspectRatio(this.width,this.height)
                        .cropOvalDimmedLayer(cropperCircleOverlay);
                if (enableRotationGesture) {
                    rxGalleryFinal.cropAllowedGestures(UCropActivity.ALL, UCropActivity.ALL, UCropActivity.ALL);
                }
            }

            rxGalleryFinal.radio()
                        .subscribe(new RxBusResultDisposable<ImageRadioResultEvent>() {
                            @Override
                            protected void onEvent(ImageRadioResultEvent imageRadioResultEvent) throws Exception {
                                if(!cropping) {
                                    Log.i("ReactNative", "sing onEvent");
                                    ImageCropBean result = imageRadioResultEvent.getResult();
                                    WritableArray resultArr = new WritableNativeArray();
                                    resultArr.pushMap(getImage(activity, result));
                                    mPickerPromise.resolve(resultArr);
                                }
                            }
                        }).openGallery();

        } else {
            rxGalleryFinal
                    .multiple()
                    .maxSize(maxFiles)
                    .subscribe(new RxBusResultDisposable<ImageMultipleResultEvent>() {
                        @Override
                        protected void onEvent(ImageMultipleResultEvent imageMultipleResultEvent) throws Exception {
                            Log.i("ReactNative","mutiple onEvent");
                            List<MediaBean> list = imageMultipleResultEvent.getResult();
                            WritableArray resultArr = new WritableNativeArray();
                            for(MediaBean bean:list){
                                resultArr.pushMap(getImage(activity,bean));
                            }
                            mPickerPromise.resolve(resultArr);
                        }
                    })
                    .openGallery();
        }
    }

    private String getBase64StringFromFile(String absoluteFilePath) {
        InputStream inputStream;

        try {
            inputStream = new FileInputStream(new File(absoluteFilePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        byte[] bytes;
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        bytes = output.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }


    private BitmapFactory.Options validateImage(String path) throws Exception {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inDither = true;

        BitmapFactory.decodeFile(path, options);

        if (options.outMimeType == null || options.outWidth == 0 || options.outHeight == 0) {
            throw new Exception("Invalid image selected");
        }

        return options;
    }


}