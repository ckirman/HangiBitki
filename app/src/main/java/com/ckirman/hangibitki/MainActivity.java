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
import com.ckirman.hangibitki.ml.ModelBitkiler;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public MainActivity()  {
    }
    private static final int PERMISSIONS_COUNT=2;
    private static final int REQUEST_PERMISSIONS=12345;
    private static final String[] PERMISSIONS={Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int REQUEST_PICK_IMAGE=1234;
    private ImageView[][] imageViewBitkiler;
    private ImageView imageViewMain;
    private static final int REQUEST_IMAGE_CAPTURE=1012;
    private static final String appID="HangiBitki";
    private Uri imageUri;
    private TextView[] textViews;
    private int[][] bitkilerid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textViews = new TextView[]{findViewById(R.id.olabilecekBitkiler1), findViewById(R.id.olabilecekBitkiler2), findViewById(R.id.olabilecekBitkiler3)};
        imageViewBitkiler =new ImageView[][]{{findViewById(R.id.obImg1_1),findViewById(R.id.obImg1_2),findViewById(R.id.obImg1_3),findViewById(R.id.obImg1_4)},
                {findViewById(R.id.obImg2_1),findViewById(R.id.obImg2_2),findViewById(R.id.obImg2_3),findViewById(R.id.obImg2_4)},
                {findViewById(R.id.obImg3_1),findViewById(R.id.obImg3_2),findViewById(R.id.obImg3_3),findViewById(R.id.obImg3_4)}};
       bitkilerid=new int[][]{{R.drawable.ates_dikeni_0,R.drawable.ates_dikeni_1,R.drawable.ates_dikeni_2,R.drawable.ates_dikeni_3},
               {R.drawable.dag_cilegi_0,R.drawable.dag_cilegi_1,R.drawable.dag_cilegi_2,R.drawable.dag_cilegi_3},
               {R.drawable.filamingo_cicegi_0,R.drawable.filamingo_cicegi_1,R.drawable.filamingo_cicegi_2,R.drawable.filamingo_cicegi_3},
               {R.drawable.gelincik_0,R.drawable.gelincik_1,R.drawable.gelincik_2,R.drawable.gelincik_3},
               {R.drawable.kadife_cicegi_0,R.drawable.kadife_cicegi_1,R.drawable.kadife_cicegi_2,R.drawable.kadife_cicegi_3},
               {R.drawable.karahindiba_0,R.drawable.karahindiba_1,R.drawable.karahindiba_2,R.drawable.karahindiba_3},
               {R.drawable.lale_0,R.drawable.lale_1,R.drawable.lale_2,R.drawable.lale_3},
               {R.drawable.lavanta_0,R.drawable.lavanta_1,R.drawable.lavanta_2,R.drawable.lavanta_3},
               {R.drawable.nar_0,R.drawable.nar_1,R.drawable.nar_2,R.drawable.nar_3},
               {R.drawable.sardunya_0,R.drawable.sardunya_1,R.drawable.sardunya_2,R.drawable.sardunya_3},
               {R.drawable.seflara_0,R.drawable.seflara_1,R.drawable.seflara_2,R.drawable.seflara_3},
               {R.drawable.yonca_0,R.drawable.yonca_1,R.drawable.yonca_2,R.drawable.yonca_3}};
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
        imageViewMain=findViewById(R.id.secilenResim);
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
        resimCekBtn.setOnClickListener(v -> {
            final Intent takePictureIntent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            //if(takePictureIntent.resolveActivity(getPackageManager())!=null){
                final File photoFile=createImageFile();
                imageUri=Uri.fromFile(photoFile);
                final SharedPreferences myPrefs=getSharedPreferences(appID,0);
                myPrefs.edit().putString("path",photoFile.getAbsolutePath()).apply();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                startActivityForResult(takePictureIntent,REQUEST_IMAGE_CAPTURE);
           // }else{
             //  Toast.makeText(MainActivity.this,"Kameranız uyumlu değil",Toast.LENGTH_SHORT).show();
           // }
        });
        final Button bitkiBulBtn=findViewById(R.id.btnHangiBitki);
        bitkiBulBtn.setOnClickListener(view -> new Thread(){
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void run(){
                runOnUiThread(() -> {
                    try {
                        ModelBitkiler model_Bitkiler = ModelBitkiler.newInstance(MainActivity.this);
                        TensorImage image = TensorImage.fromBitmap(bitmap);
                        ModelBitkiler.Outputs outputs = model_Bitkiler.process(image);
                        List<Category> probability = outputs.getProbabilityAsCategoryList();
                        probability.sort(Comparator.comparing(Category::getScore, Comparator.reverseOrder()));
                        for (int i=0; i<3; i++){
                            if((int)(probability.get(i).getScore()*100)>0)
                                textViews[i].setText("%"+ (int)(probability.get(i).getScore()*100)+" ihtimalle "+probability.get(i).getLabel());
                            else
                                textViews[i].setText("%1< ihtimalle "+probability.get(i).getLabel());
                            for(int k=0;k<4;k++) {
                                imageViewBitkiler[i][k].setImageResource(bitkilerid[getLabelIndex(probability.get(i).getLabel())][k]);
                            }
                        }
                        findViewById(R.id.olabilecekBitkilerLayout).setVisibility(View.VISIBLE);
                        model_Bitkiler.close();
                    } catch (IOException e) {
                        // TODO Handle the exception
                    }
                });

            }
        }.start());
        final Button geriBtn=findViewById(R.id.btnGeri);
        geriBtn.setOnClickListener(v -> {
            findViewById(R.id.MainScreen).setVisibility(View.VISIBLE);
            findViewById(R.id.editScreen).setVisibility(View.INVISIBLE);
            findViewById(R.id.olabilecekBitkilerLayout).setVisibility(View.INVISIBLE);
        });
    }

    private int getLabelIndex(String label) {
        switch (label){
            case "Ateş Dikeni":
                return 0;
            case "Çilek":
                return 1;
            case "Filamingo Çiçeği":
                return 2;
            case "Gelincik":
                return 3;
            case "Kadife Çiçeği":
                return 4;
            case "Karahindiba":
                return 5;
            case "Lale":
                return 6;
            case "Lavanta":
                return 7;
            case "Nar":
                return 8;
            case "Sardunya":
                return 9;
            case "Şeflara":
                return 10;
            case "Yonca":
                return 11;
            default:
                return 0;
        }
    }
    private File createImageFile(){
        final String timeStamp=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date());
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
                InputStream input;
                try {
                    input=getContentResolver().openInputStream(imageUri);
                }catch (FileNotFoundException e){
                    e.printStackTrace();
                    recreate();
                    return;
                }
                bitmap=BitmapFactory.decodeStream(input,null,bmpOptions);
                runOnUiThread(() -> {
                    imageViewMain.setImageBitmap(bitmap);
                    dialog.cancel();
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