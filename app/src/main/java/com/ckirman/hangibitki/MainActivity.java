package com.ckirman.hangibitki;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ckirman.hangibitki.ml.ModelYapraklar;
import com.ckirman.hangibitki.ml.ModelCicekler;
import com.ckirman.hangibitki.ml.ModelMeyveler;


import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public MainActivity() throws IOException {
    }
    private static final int PERMISSIONS_COUNT=2;
    private static final int REQUEST_PERMISSIONS=12345;
    private static final String[] PERMISSIONS={Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int REQUEST_PICK_IMAGE=1234;
    private ImageView imageView;
    private static final int REQUEST_IMAGE_CAPTURE=1012;
    private static final String appID="HangiBitki";
    private Uri imageUri;
    private String[] labelsYapraklar=new String[20];
    private String[] labelsCicekler=new String[20];
    private String[] labelsMeyveler=new String[20];
    private int cnt=0;
    private TextView[] textViews;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textViews = new TextView[]{findViewById(R.id.olabilecekBitkiler1), findViewById(R.id.olabilecekBitkiler2), findViewById(R.id.olabilecekBitkiler3)};
        init();
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean notPermissions(){
        for (int i=0;i<PERMISSIONS_COUNT;i++){
            if(checkSelfPermission(PERMISSIONS[i])!=PackageManager.PERMISSION_GRANTED)
                return true;
        }
        return false;
    }
    @Override
    protected void onResume(){
        super.onResume();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notPermissions()){
                requestPermissions(PERMISSIONS,REQUEST_PERMISSIONS);
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public  void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
       super.onRequestPermissionsResult(requestCode,permissions,grantResults);
       if(requestCode==REQUEST_PERMISSIONS&&grantResults.length>0){
           if(notPermissions()){
               ((ActivityManager) this.getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
               recreate();
           }
       }
    }

    private void init(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
            StrictMode.VmPolicy.Builder builder=new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }
        imageView=findViewById(R.id.secilenResim);
        if(!MainActivity.this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
            findViewById(R.id.resimCekBtn).setVisibility(View.GONE);
        }
        final Button resimSecBtn=findViewById(R.id.resimSecBtn);
        resimSecBtn.setOnClickListener(view -> {
            final Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            final Intent pickIntent=new Intent(Intent.ACTION_PICK);
            pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*");
            final Intent chooseIntent=Intent.createChooser(intent,"Select Image");
            startActivityForResult(chooseIntent,REQUEST_PICK_IMAGE);
        });
        final Button resimCekBtn=findViewById(R.id.resimCekBtn);
        resimCekBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent takePictureIntent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(takePictureIntent.resolveActivity(getPackageManager())!=null){
                    final File photoFile=createImageFile();
                    imageUri=Uri.fromFile(photoFile);
                    final SharedPreferences myPrefs=getSharedPreferences(appID,0);
                    myPrefs.edit().putString("path",photoFile.getAbsolutePath()).apply();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                    startActivityForResult(takePictureIntent,REQUEST_IMAGE_CAPTURE);
                }else{
                   Toast.makeText(MainActivity.this,"Kameranız uyumlu değil",Toast.LENGTH_SHORT).show();
                }
            }
        });
        final Button yaprakBulBtn=findViewById(R.id.btnYaprak);
        yaprakBulBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(){
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    public void run(){
                        try {
                            ModelYapraklar model_yaprak = ModelYapraklar.newInstance(MainActivity.this);
                            TensorImage image = TensorImage.fromBitmap(bitmap);
                            ModelYapraklar.Outputs outputs = model_yaprak.process(image);
                            List<Category> probability = outputs.getProbabilityAsCategoryList();
                            probability.sort(Comparator.comparing(Category::getScore, Comparator.reverseOrder()));
                            for (int i=0; i<3; i++)
                                textViews[i].setText(probability.get(i).getLabel().toUpperCase()+":"+probability.get(i).getScore());
                            model_yaprak.close();
                        } catch (IOException e) {
                            // TODO Handle the exception
                        }
                    }
                }.start();

            }
        });
        final Button geriBtn=findViewById(R.id.btnGeri);
        geriBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.MainScreen).setVisibility(View.VISIBLE);
                findViewById(R.id.editScreen).setVisibility(View.INVISIBLE);
            }
        });
        final Button cicekBulBtn=findViewById(R.id.btnCicek);
        cicekBulBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(){
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    public void run(){
                        try {
                            ModelCicekler model_cicek = ModelCicekler.newInstance(MainActivity.this);
                            TensorImage image = TensorImage.fromBitmap(bitmap);
                            ModelCicekler.Outputs outputs = model_cicek.process(image);
                            List<Category> probability = outputs.getProbabilityAsCategoryList();
                            probability.sort(Comparator.comparing(Category::getScore, Comparator.reverseOrder()));
                            for (int i=0; i<3; i++)
                                textViews[i].setText(probability.get(i).getLabel()+ probability.get(i).getScore());
                            model_cicek.close();
                        } catch (IOException e) {
                            // TODO Handle the exception
                        }
                    }
                }.start();

            }
        });
        final Button meyveBulBtn=findViewById(R.id.btnMeyve);
        meyveBulBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(){
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    public void run(){
                        try {
                            ModelMeyveler model_meyve = ModelMeyveler.newInstance(MainActivity.this);
                            TensorImage image = TensorImage.fromBitmap(bitmap);
                            ModelMeyveler.Outputs outputs = model_meyve.process(image);
                            List<Category> probability = outputs.getProbabilityAsCategoryList();
                            probability.sort(Comparator.comparing(Category::getScore, Comparator.reverseOrder()));
                            for (int i=0; i<3; i++)
                                textViews[i].setText(probability.get(i).getLabel()+ probability.get(i).getScore());
                            // ModelClassifierPlants model = ModelClassifierPlants.newInstance(getApplicationContext());
                            // Runs model inference and gets result.
                            //TensorBuffer OutputsFeature=outputs.getProbabilityAsTensorBuffer();
                            //bitkiListesi.setText(getMaxFloat(OutputsFeature.getFloatArray())+labelsMeyveler[getMaxInt(OutputsFeature.getFloatArray())]);
                            model_meyve.close();
                        } catch (IOException e) {
                            // TODO Handle the exception
                        }
                    }
                }.start();

            }
        });
    }

    private int getMaxInt(float[] arr) {
        int max=0;
        for (int i=0;i<arr.length;i++){
            if(arr[i]<arr[max])
                max=i;
        }
        return max;
    }
    private float getMaxFloat(float[] arr) {
        int max=0;
        for (int i=0;i<arr.length;i++){
            if(arr[i]<arr[max])
                max=i;
        }
        return arr[max];
    }
    private float[] sortedArray(float[] arr){
        for (int i = 0; i < arr.length - 1; i++)
            for (int j = 0; j < arr.length - i - 1; j++)
                if (arr[j] > arr[j + 1]) {
                    // swap arr[j+1] and arr[j]
                    float temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                }
        return arr;
    }

    private File createImageFile(){
        final String timeStamp=new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final String imageFileName="/JPEG_"+timeStamp+".jpg";
        final File storageDir= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(storageDir+imageFileName);
    }
    private boolean editMode=false;
    private Bitmap bitmap;
    private int width=0;
    private int height=0;
    private static final int MAX_PIXEL_COUNT=2048;
    private int[] pixels;
    private int pixelCount=0;
    @Override
    public void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK)
            return;
        if (requestCode==REQUEST_IMAGE_CAPTURE){
            if (imageUri==null) {
                final SharedPreferences p = getSharedPreferences(appID, 0);
                final String path = p.getString("path", "");
                if (path.length() < 1) {
                    recreate();
                    return;
                }
                imageUri=Uri.parse("file://"+path);
            }
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,imageUri));
        }else if(data==null){
            recreate();
            return;
        }else if (requestCode==REQUEST_PICK_IMAGE){
            imageUri=data.getData();
        }
        final ProgressDialog dialog=ProgressDialog.show(MainActivity.this,"Yükleniyor","Lütfen Bekleyin",true);
        editMode=true;
        findViewById(R.id.MainScreen).setVisibility(View.GONE);
        findViewById(R.id.editScreen).setVisibility(View.VISIBLE);
        new Thread(){
            public void run(){
                bitmap=null;
                final BitmapFactory.Options bmpOptions=new BitmapFactory.Options();
                bmpOptions.inBitmap=bitmap;
                bmpOptions.inJustDecodeBounds=true;
                try (InputStream input=getContentResolver().openInputStream(imageUri)){
                   bitmap=BitmapFactory.decodeStream(input,null,bmpOptions);
                }catch (IOException e){
                    e.printStackTrace();
                }
                bmpOptions.inJustDecodeBounds=false;
                width=bmpOptions.outWidth;
                height=bmpOptions.outHeight;
                int resizeScale=1;
                if(width>MAX_PIXEL_COUNT){
                    resizeScale=width/MAX_PIXEL_COUNT;
                }else if(height>MAX_PIXEL_COUNT){
                    resizeScale=height/MAX_PIXEL_COUNT;
                }
                if(width/resizeScale>MAX_PIXEL_COUNT||height/resizeScale>MAX_PIXEL_COUNT)
                    resizeScale++;
                bmpOptions.inSampleSize=resizeScale;
                InputStream input=null;
                try {
                    input=getContentResolver().openInputStream(imageUri);
                }catch (FileNotFoundException e){
                    e.printStackTrace();
                    recreate();
                    return;
                }
                bitmap=BitmapFactory.decodeStream(input,null,bmpOptions);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(bitmap);
                        dialog.cancel();
                    }
                });
                width=bitmap.getWidth();
                height=bitmap.getHeight();
                bitmap=bitmap.copy(Bitmap.Config.ARGB_8888,true);
                pixelCount=width*height;
                pixels=new int[pixelCount];
                bitmap.getPixels(pixels,0,width,0,0,width,height);
            }
        }.start();

    }
}