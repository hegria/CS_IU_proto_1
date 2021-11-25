package com.example.CS_IU_proto_1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ResultActivity extends AppCompatActivity implements GLSurfaceView.Renderer {

    GLSurfaceView glView;
    Bitmap image;

    ArrayList<Ellipse> ellipses;

    DrawText drawText;

    float[] projMX;
    float[] viewMX;
    float[] cameratrans;

    int width = 1;
    int height = 1;

    int maxVal, minVal;

    float offset;

    //경계 값 (원래라면 각각 15, 30)
    final int b1 = 2;
    final int b2 = 3;

    boolean isBusy = false;
    boolean correctionmode = false;
    boolean isAlready = false;
    boolean isEdit = false;

    ArrayList<String> taglist;
    Map<String, GroupInfo> grouplist;

    ImageButton btnDrawer;
    DrawerLayout drawerLayout;
    TextView txtDate;
    EditText txtAddress;
//    EditText txtFilename;
    EditText txtSpecies;
    EditText txtHuman;
    EditText txtlocation;
    AutoCompleteTextView txtTag;
    TextView textCont;
    TextView textAvgdia;
    TextView textvolumn;
    TextView textnum;
    Switch switch1, switch2, switch3;
    RangeSeekBar<Integer> seekBar;
    SeekBar seekBar2;
    Button correctionbutton;
    Button delbutton;
    Button savebutton;
    Button editbutton;
    Plane plane;
    Ellipse nowellipse;
    Ellipse tempellipse;
    int selected_index;
    int temp_index;
    float nowdiameter;
    int nowcount;
    float nowvol;

    BackgroundImage backgroundImage;
    ExecutorService worker;
    EllipsePool ellipsePool;

    //키보드 수동 제어
    InputMethodManager inputMethodManager;

    long backKeyPressedTime;
    boolean tagFirstPressed = false;

    //for Long touch
    static int LONG_PRESS_TIME = 300;

    long starttime = 0;

    //가이드라인 진행 상태
    private enum Gl_State {Idle, Filtering, VisibilityControl, Adding1, Adding2, Editing, SAVING1, SAVING2, SAVING3}

    Gl_State gl_state = Gl_State.Idle;
    GuideLine guideLine;
    PrefManager pf;

    String filename = "";
    String datestr = "";
    String addressstr = "";
    String locationstr = "";
    String templocationstr = "";
    String speices = "";
    String human = "";
    String group = "";
    String num = "";


    //1. file IO
    //1-1 image save
    //1-2 make json file
    //1-a Load 기능
    //2. edit button / Save button
    // visibilty 설정
    //3. Plane text로 변경
    //4. Doker

    //locaiton
    LocationManager lm;
    Location location;
    Geocoder geocoder;

    //time
    long now;
    Date date;
    SimpleDateFormat dateFormat;

    private TimberinfoDB timberinfoDB = null;
    private Context context;

    //Gson
    Gson gson;

    // 재장 및 부피

    EditText editLongivity;
    boolean haslongivity = false;
    float longivity = 0;
    float volumn;

    int from;

    @SuppressLint({"ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        worker = Executors.newSingleThreadExecutor();
        setContentView(R.layout.activity_result);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},100);
        }

        timberinfoDB = TimberinfoDB.getInstance(this);
        context = getApplicationContext();


        guideLine = new GuideLine(this);
        pf = new PrefManager(this);
        if (pf.isFirstTimeLaunch2()) {
            guideLine.gl6();
            gl_state = Gl_State.Filtering;
        }

        // Image랑 Ellipse를 받아내고, 이를 다시 그려내야함.
        // 그려내는 부분에서 차라리 Ellipse를 평면에 정사영 시키는 편이 낫지 않을까?
        // Background 같은경우도 새로운 자료형을 만들어내야함( Image를 Bitmap을 통해서 그려낼 수 있는
        Intent intent = getIntent();
        from = intent.getIntExtra("from",0);
        //1은 그냥 2는 로딩
        ellipses = intent.getParcelableArrayListExtra("Ellipse");
        plane = intent.getParcelableExtra("plane");
        projMX = intent.getFloatArrayExtra("projMat");
        viewMX = intent.getFloatArrayExtra("viewMat");
        cameratrans = intent.getFloatArrayExtra("cameratrans");
        offset = intent.getFloatExtra("offset",0);
        byte[] byteArray = intent.getByteArrayExtra("image");
        image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);



        editLongivity = findViewById(R.id.editTextTextPersonName);
        txtSpecies = findViewById(R.id.txtSpecies);
        txtHuman = findViewById(R.id.txtHuman2);

        switch (from){
            case 1:
                lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


                location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if(location == null){
                    location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if(location == null){
                    locationstr = "";
                    Toast.makeText(this, "위치를 찾지 못했습니다.\n직접 입력해주세요.", Toast.LENGTH_SHORT).show();

                }else{

                    geocoder = new Geocoder(this);
                    List<Address> list =null;
                    try{

                        list = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                    if(!list.isEmpty()) {
                        if (list.get(0).getLocality() == null) {
                            templocationstr = list.get(0).getAdminArea() + " " + list.get(0).getAddressLine(0).split(" ")[2];
                        } else {
                            templocationstr = list.get(0).getLocality() + " " + list.get(0).getThoroughfare();
                        }
                        locationstr = list.get(0).getAddressLine(0);
                    }
                }

                now = System.currentTimeMillis();
                date = new Date(now);
                dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                datestr = dateFormat.format(date);
                //filename = templocationstr +"_"+datestr;
                Log.i("a", addressstr);

                break;
            case 2:
                haslongivity = true;
                speices = intent.getStringExtra("speices");
                datestr = intent.getStringExtra("date");
                locationstr = intent.getStringExtra("location");
                addressstr = intent.getStringExtra("space");
                longivity = intent.getFloatExtra("long",0);
                filename = intent.getStringExtra("filename");
                human = intent.getStringExtra("human");
                group = intent.getStringExtra("tag");
                // now date
                CharSequence cs = Float.toString(longivity);
                editLongivity.setText(cs);
                txtSpecies.setText(speices);
                txtHuman.setText(human);
                num = filename.split("_")[filename.split("_").length-1];
                break;
            default:
                break;
        }




        Log.i("img",""+byteArray.length);
        glView = (GLSurfaceView) findViewById(R.id.subsurface);
        glView.setPreserveEGLContextOnPause(true);
        glView.setEGLContextClientVersion(2);
        glView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glView.setRenderer(this);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        glView.setWillNotDraw(false);

        btnDrawer = findViewById(R.id.btnDrawer);
        drawerLayout = findViewById(R.id.drawerLayout);
        txtAddress = findViewById(R.id.txtAddress2);
        txtlocation = findViewById(R.id.txtlocation2);
        txtTag = findViewById(R.id.txtTagedit);
        txtDate = findViewById(R.id.txtDate);
        textCont = findViewById(R.id.text_logCount);
        textAvgdia = findViewById(R.id.text_avgDiameter);
        textvolumn = findViewById(R.id.text_avgDiameter2);
        switch1 = findViewById(R.id.switch1);
        switch2 = findViewById(R.id.switch2);
        switch3 = findViewById(R.id.switch3);
        seekBar = findViewById(R.id.seekBar);
        seekBar2 = findViewById(R.id.seekBar2);
        textnum = findViewById(R.id.txtTag2);
        seekBar2.setVisibility(View.INVISIBLE);
        correctionbutton = findViewById(R.id.correction);
        correctionbutton.setVisibility(View.INVISIBLE);

        txtTag.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);


        gson = new Gson();
        File file = getBaseContext().getFileStreamPath("_mytags.json");
        if(file.exists()){
            try {
                JsonReader jsonReader = new JsonReader(new InputStreamReader(openFileInput("_mytags.json"),"UTF-8"));
                jsonReader.setLenient(true);
                Type listtype = new TypeToken<Map<String, GroupInfo>>() {}.getType();
                grouplist = gson.fromJson(jsonReader,listtype);
                if(grouplist!=null){
                    Set<String> a= grouplist.keySet();
                    taglist = new ArrayList<>(a);
                    txtTag.setAdapter(new ArrayAdapter<String>(this,R.layout.support_simple_spinner_dropdown_item,taglist));
                }else{
                    taglist = new ArrayList<>();
                    grouplist = new HashMap<>();
                }

            } catch (UnsupportedEncodingException | FileNotFoundException e) {
                e.printStackTrace();
            }
        }else{
            taglist = new ArrayList<>();
            grouplist = new HashMap<>();
        }


//        txtFilename.setText(filename);
        txtDate.setText("날짜:   " + datestr);
        txtlocation.setText(locationstr);
        txtAddress.setText(addressstr);
        txtTag.setText(group);
        textnum.setText(num);

        int inType = txtTag.getInputType();

        txtTag.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP){
                    if(!tagFirstPressed) {
                        Toast.makeText(getApplicationContext(), "한번 더 터치하여 직접 입력", Toast.LENGTH_SHORT).show();
                        //드랍다운 메뉴는 보여주고
                        txtTag.showDropDown();
                        //키보드 숨기기
                        txtTag.setInputType(InputType.TYPE_NULL);
                        tagFirstPressed = true;
                    }else{
                        //키보드 보이기
                        txtTag.setInputType(inType);
                        tagFirstPressed = false;
                    }
                }
                return false;
            }
        });

        txtTag.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String tempstr = s.toString();
                if(!tempstr.isEmpty()){
                    if(taglist.contains(tempstr)){
                        if(txtlocation.getText().toString().isEmpty()){
                            txtlocation.setText(grouplist.get(tempstr).location);
                        }
                        txtHuman.setText(grouplist.get(tempstr).human);
                        txtSpecies.setText(grouplist.get(tempstr).spices);
                        filename = tempstr+"_"+grouplist.get(tempstr).maxnum;
                        textnum.setText(Integer.toString(grouplist.get(tempstr).maxnum));
                        txtAddress.setText(grouplist.get(tempstr).address);
                    }else{
                        textnum.setText("1");
                        filename = tempstr+"_1";
                    }
                }
            }
        });


        editLongivity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //변화
            }

            @Override
            public void afterTextChanged(Editable s) {
                //입력끝끝
                if(!s.toString().isEmpty()) {
                    longivity = Float.parseFloat(s.toString());
                    if (longivity != 0) {
                        haslongivity = true;
                        setText();
                    }
                }
            }
       });


        // add button
        correctionbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!correctionmode){
                    correctionmode = true;
                    Ray originray = Myutil.GenerateRay(glView.getMeasuredWidth()/2,glView.getMeasuredHeight()/2,glView.getMeasuredWidth(),glView.getMeasuredHeight(),projMX,viewMX,cameratrans);
                    nowellipse = new Ellipse(originray, plane,projMX,viewMX);
                    nowellipse.isEdited = true;
                    ellipses.add(nowellipse);
                    selected_index = ellipses.indexOf(nowellipse);
                    correctionbutton.setText("Apply");
                    delbutton.setText("DEL");
                    seekBar2.setVisibility(View.VISIBLE);

                }else{
                    correctionmode = false;

                    if(isAlready){
                        tempellipse = null;
                        ellipses.remove(temp_index);
                        isAlready = false;
                    }

                    nowellipse.isEdited =false;
                    nowellipse = null;
                    correctionbutton.setText("ADD");
                    delbutton.setText("DONE");
                    seekBar2.setVisibility(View.INVISIBLE);
                }
                setText();
            }
        });
        delbutton = findViewById(R.id.correction3);
        delbutton.setVisibility(View.INVISIBLE);
        delbutton.setText("DONE");

        delbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(correctionmode){
                    //which means del
                    correctionmode = false;
                    nowellipse = null;

                    if(isAlready){
                        tempellipse = null;
                        ellipses.remove(temp_index);
                        isAlready = false;
                        selected_index -= 1;
                    }
                    ellipses.remove(selected_index);
                    correctionbutton.setText("ADD");
                    delbutton.setText("DONE");
                    seekBar2.setVisibility(View.INVISIBLE);
                    correctionbutton.setVisibility(View.VISIBLE);
                }else{
                    //which means done

                    isEdit = false;
                    delbutton.setVisibility(View.INVISIBLE);
                    editbutton.setVisibility(View.VISIBLE);
                    savebutton.setVisibility(View.VISIBLE);
                }
            }
        });

        editbutton= findViewById(R.id.Editbtn);
        editbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isEdit = true;
                editbutton.setVisibility(View.INVISIBLE);
                savebutton.setVisibility(View.INVISIBLE);
                correctionbutton.setVisibility(View.VISIBLE);
                delbutton.setVisibility(View.VISIBLE);
            }
        });

        txtlocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(!s.toString().isEmpty()){
                    locationstr = s.toString();
                    //filename = s.toString() +"_"+datestr;
                    //txtFilename.setText(filename);
                }
            }
        });


        savebutton = findViewById(R.id.SaveButton);
        savebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                speices = txtSpecies.getText().toString();
                human = txtHuman.getText().toString();
                addressstr = txtAddress.getText().toString();
                group = txtTag.getText().toString();

                if(addressstr.isEmpty()){
                    Toast.makeText(ResultActivity.this,"촬영 장소를 입력해 주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(speices.isEmpty()){
                    Toast.makeText(ResultActivity.this, "수종을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(longivity ==0){
                    Toast.makeText(ResultActivity.this, "재장을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(human.isEmpty()){
                    Toast.makeText(ResultActivity.this, "검척자의 이름을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(locationstr.isEmpty()){
                    Toast.makeText(ResultActivity.this,"위치를 입력해 주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(group.isEmpty()){
                    Toast.makeText(ResultActivity.this,"태그를 입력해 주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!taglist.contains(group)) {
                    GroupInfo groupInfo = new GroupInfo();
                    groupInfo.location = locationstr;
                    groupInfo.address = addressstr;
                    groupInfo.human = human;
                    groupInfo.spices = speices;
                    groupInfo.maxnum = 2;

                    grouplist.put(group, groupInfo);
                }else{
                    grouplist.get(group).maxnum = grouplist.get(group).maxnum +1;

                }

                File file2 = getBaseContext().getFileStreamPath("_mytags.json");
                if(file2.exists()){
                    file2.delete();
                }
                try {
                    JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(openFileOutput("_mytags.json",Context.MODE_PRIVATE),"UTF-8"));
                    Type listtype = new TypeToken<Map<String,GroupInfo>>() {}.getType();
                    gson.toJson(grouplist,listtype,jsonWriter);
                    jsonWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                FileOutputStream fos_img = null;



                JsonObject jsonObject = new JsonObject();
                jsonObject.add("plane",gson.toJsonTree(plane).getAsJsonObject());
                jsonObject.add("ellipses",gson.toJsonTree(ellipses).getAsJsonArray());
                jsonObject.add("projMX",gson.toJsonTree(projMX).getAsJsonArray());
                jsonObject.add("viewMX",gson.toJsonTree(viewMX).getAsJsonArray());
                jsonObject.add("cameratrans",gson.toJsonTree(cameratrans).getAsJsonArray());
                jsonObject.addProperty("offset",offset);

                // TODO 이미 info가 있는지 확인해야함.

                Timberinfo timberinfo = new Timberinfo();
                timberinfo.filename = filename;
                timberinfo.spiece = speices;
                timberinfo.date =datestr;
                timberinfo.location = locationstr;
                timberinfo.space = addressstr;
                timberinfo.longivity = longivity;
                timberinfo.human = human;
                timberinfo.count = nowcount;
                timberinfo.avgDiameter = nowdiameter;
                timberinfo.volumn = nowvol;
                timberinfo.tag = group;


                class InsertRunnable implements Runnable{
                    Timberinfo timberinfo;
                    public InsertRunnable(Timberinfo _timberinfo){
                        timberinfo = _timberinfo;
                    }
                    @Override
                    public void run() {
                        TimberinfoDB.getInstance(context).timberinfoDao().delete(filename);
                        TimberinfoDB.getInstance(context).timberinfoDao().insertAll(timberinfo);
                    }
                }
                InsertRunnable insertRunnable = new InsertRunnable(timberinfo);
                Thread t = new Thread(insertRunnable);
                t.start();


                File file = getBaseContext().getFileStreamPath(filename+".json");
                if(file.exists()){
                    try {
                        file.delete();
                        JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(openFileOutput(filename+".json", Context.MODE_PRIVATE),"UTF-8"));
                        gson.toJson(jsonObject,jsonWriter);
                        jsonWriter.close();
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(), "저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                    Toast.makeText(getApplicationContext(), "저장되었습니다.", Toast.LENGTH_SHORT).show();

                }else{
                    try {
                        // 파일 이름 변경 되었을 때 예외 처리
                        JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(openFileOutput(filename+".json", Context.MODE_PRIVATE),"UTF-8"));
                        gson.toJson(jsonObject,jsonWriter);
                        fos_img = openFileOutput(filename+".JPG", Context.MODE_PRIVATE);
                        image.compress(Bitmap.CompressFormat.JPEG,100,fos_img);
                        jsonWriter.close();
                        Toast.makeText(getApplicationContext(), "저장되었습니다.", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(), "저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }



            }
        });


        glView.setOnTouchListener((View view, MotionEvent event) -> {


            if(pf.isFirstTimeLaunch2())
                return false;

            float xPx, yPx;
            int screenWidth, screenHeight;
            xPx = event.getX();
            yPx = event.getY();
            screenWidth = glView.getMeasuredWidth();
            screenHeight = glView.getMeasuredHeight();


            float x = 2.0f * xPx / screenWidth - 1.0f;
            float y = 1.0f - 2.0f * yPx / screenHeight;
            Log.i("xpx,ypx", Float.toString(xPx)+ Float.toString(yPx));
            int action = event.getAction();
            switch (action){
                case MotionEvent.ACTION_UP:
                    long deltatime = event.getEventTime() - event.getDownTime();
                    Log.i("Time",""+deltatime);
                    if(deltatime<LONG_PRESS_TIME||!isEdit){

                        if(!correctionmode){

                            worker.execute(()->{
                                float minDistanceSq = Float.MAX_VALUE;
                                int idx = -1;
                                int i = 0;
                                float[] point;
                                for(Ellipse ellipse : ellipses){
                                    point = new float[]{ellipse.resultpivot[0], ellipse.resultpivot[1]};
                                    float distanceSq = (x-point[0])*(x-point[0]) + (y-point[1])*(y-point[1]);
                                    Log.i("distance",""+distanceSq);
                                    if(distanceSq<0.01f&& distanceSq<minDistanceSq){
                                        idx = i;
                                        minDistanceSq = distanceSq;
                                    }
                                    i++;
                                }
                                if(idx != -1){
                                    ellipses.get(idx).istoggled = !ellipses.get(idx).istoggled;
                                    setText();
                                }
                            });
                        }else{
                            //button으로 넘겨야함.
                            Ray ray = Myutil.GenerateRay(xPx,yPx,screenWidth,screenHeight,projMX,viewMX,cameratrans);
                            nowellipse.movepivot(ray,plane,projMX,viewMX);
                        }
                    }else {

                        if (!correctionmode&&isEdit) {
                            worker.execute(() -> {
                                float minDistanceSq = Float.MAX_VALUE;
                                int idx = -1;
                                int i = 0;
                                float[] point;
                                for (Ellipse ellipse : ellipses) {
                                    point = new float[]{ellipse.resultpivot[0], ellipse.resultpivot[1]};
                                    float distanceSq = (x - point[0]) * (x - point[0]) + (y - point[1]) * (y - point[1]);
                                    Log.i("distance", "" + distanceSq);
                                    if (distanceSq < 0.01f && distanceSq < minDistanceSq) {
                                        idx = i;
                                        minDistanceSq = distanceSq;
                                    }
                                    i++;
                                }
                                if (idx != -1) {
                                    // TODO 여길 바꿔야함.
                                    correctionmode = true;
                                    isAlready = true;
                                    selected_index = idx;
                                    tempellipse = ellipses.get(idx);
                                    try {
                                        nowellipse = tempellipse.clone();
                                    } catch (CloneNotSupportedException e) {
                                        e.printStackTrace();
                                    }
                                    tempellipse.istoggled = false;
                                    ellipses.add(nowellipse);
                                    selected_index = ellipses.indexOf(nowellipse);
                                    temp_index = idx;
                                    nowellipse.isEdited = true;
                                    runOnUiThread(()->{
                                        correctionbutton.setText("Apply");
                                        delbutton.setText("DEL");
                                        correctionbutton.setVisibility(View.VISIBLE);
                                        seekBar2.setVisibility(View.VISIBLE);
                                        seekBar2.setProgress((int) (nowellipse.size2 * 10));

                                    });
                                }
                            });
                        }
                        setText();

                    }
            }
            return true;
        });

        switch1.setOnClickListener(l -> {
            if (switch1.isChecked()) {
                for(Ellipse ellipse : ellipses){
                    seekBar.setSelectedMinValue(b1);
                    if(ellipse.size < b1)
                        ellipse.istoggled = true;
                }
            } else {
                for(Ellipse ellipse : ellipses){
                    if(ellipse.size < b1)
                        ellipse.istoggled = false;
                }
            }
            setRange();
            setText();
        });

        switch2.setOnClickListener(l -> {
            if (switch2.isChecked()) {
                for(Ellipse ellipse : ellipses){
                    if(ellipse.size >= b1 && ellipse.size < b2)
                        ellipse.istoggled = true;
                }
            } else {
                for(Ellipse ellipse : ellipses){
                    if(ellipse.size >= b1 && ellipse.size < b2)
                        ellipse.istoggled = false;
                }
            }
            setRange();
            setText();
        });

        switch3.setOnClickListener(l -> {
            if (switch3.isChecked()) {
                for(Ellipse ellipse : ellipses){
                    if(ellipse.size >= b2)
                        ellipse.istoggled = true;
                }
            } else {
                for(Ellipse ellipse : ellipses){
                    if(ellipse.size >= b2)
                        ellipse.istoggled = false;
                }
            }
            setRange();
            setText();
        });

        maxVal = 0;
        minVal = 100;

        for(Ellipse ellipse : ellipses){
            if(ellipse.size < minVal)
                minVal = ellipse.size;
            if(ellipse.size > maxVal)
                maxVal = ellipse.size;
        }

        seekBar.setRangeValues(minVal, maxVal);
        seekBar.setOnRangeSeekBarChangeListener((bar, minValue, maxValue) -> {
            int selectedMinVal = (Integer)(bar.getSelectedMinValue());
            int selectedMaxVal = (Integer)(bar.getSelectedMaxValue());

            for(Ellipse ellipse : ellipses){
                ellipse.istoggled =
                        (ellipse.size >= selectedMinVal) && (ellipse.size <= selectedMaxVal);
            }
            setText();
        });
        seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(correctionmode){
                    nowellipse.changerad(seekBar.getProgress()/10f,plane);
                    setText();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        btnDrawer.setOnClickListener(v -> {
            if (!drawerLayout.isDrawerOpen(Gravity.RIGHT)) {
                drawerLayout.openDrawer(Gravity.RIGHT) ;
            }else {
                drawerLayout.closeDrawer(Gravity.RIGHT);
                tagFirstPressed = false;
            }

        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        glView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glView.onResume();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        backgroundImage = new BackgroundImage();
        backgroundImage.updatImage(image);
        drawText = new DrawText(offset);
        drawText.setTexture(width,height);
        ellipsePool = new EllipsePool(100);
        setText();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;
        GLES20.glViewport(0, 0, width, height);

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if(!isBusy) {
            isBusy =true;
            glView.queueEvent(() -> {
                ellipsePool.clear();
                drawText.clearEllipses();
                for (int i =0 ; i<ellipses.size();i++) {
                    if(ellipsePool.isFull())
                        ellipsePool.addEllipse(ellipses.get(i));
                    else
                        ellipsePool.setEllipse(ellipses.get(i));
                    if(ellipses.get(i).istoggled) {
                        drawText.setEllipses(ellipses.get(i));
                    }
                }
                drawText.setTexture(width, height);
                isBusy = false;
            });
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        backgroundImage.draw();

        for(int i = 0; i < ellipsePool.useCount; i++) {
            ellipsePool.drawEllipses.get(i).draw(viewMX, projMX,offset);
        }
        drawText.draw();
    }

    void setRange(){
        if(switch1.isChecked() && switch2.isChecked() && switch3.isChecked()) {
            seekBar.setSelectedMinValue(minVal);
            seekBar.setSelectedMaxValue(maxVal);
        }else if(switch1.isChecked() && switch2.isChecked() && !switch3.isChecked()) {
            seekBar.setSelectedMinValue(minVal);
            seekBar.setSelectedMaxValue(b2-1);
        }else if(switch1.isChecked() && !switch2.isChecked() && switch3.isChecked()) {
            seekBar.setSelectedMinValue(minVal);
            seekBar.setSelectedMaxValue(maxVal);
        }else if(!switch1.isChecked() && switch2.isChecked() && switch3.isChecked()) {
            seekBar.setSelectedMinValue(b1);
            seekBar.setSelectedMaxValue(maxVal);
        }else if(switch1.isChecked() && !switch2.isChecked() && !switch3.isChecked()) {
            seekBar.setSelectedMinValue(minVal);
            seekBar.setSelectedMaxValue(b1-1);
        }else if(!switch1.isChecked() && switch2.isChecked() && !switch3.isChecked()) {
            seekBar.setSelectedMinValue(b1);
            seekBar.setSelectedMaxValue(b2-1);
        }else if(!switch1.isChecked() && !switch2.isChecked() && switch3.isChecked()) {
            seekBar.setSelectedMinValue(b2);
            seekBar.setSelectedMaxValue(maxVal);
        }else if(!switch1.isChecked() && !switch2.isChecked() && !switch3.isChecked()) {
            seekBar.setSelectedMinValue(minVal);
            seekBar.setSelectedMaxValue(minVal);
        }
    }

    @SuppressLint("DefaultLocale")
    void setText(){
        int count = 0;
        float dia = 0;
        volumn = 0;
        for (Ellipse ellipse : ellipses) {
            if(ellipse.istoggled){
                count++;
                dia += ellipse.size;
                if(haslongivity){
                    volumn += ellipse.size*ellipse.size*Math.PI*longivity;
                }
            }
        }
        if(count != 0)
            dia /= count;
        int finalCount = count;
        float finalDia = dia;
        float finalVolumn = volumn / 3338.450667f;
        nowcount = count;
        nowdiameter = dia;
        nowvol = volumn / 3338.450667f;
        runOnUiThread(() -> {
            textCont.setText(String.format("개수 : %d개", finalCount));
            textAvgdia.setText(String.format("평균 직경 : %.1fcm", finalDia));
            if(haslongivity){
                textvolumn.setText(String.format("부피 : %.2f 재",finalVolumn));
            }
        });
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_UP) {
            if (drawerLayout.isDrawerOpen(Gravity.RIGHT)) {
                drawerLayout.closeDrawer(Gravity.RIGHT);
                tagFirstPressed = false;
            }
            switch (gl_state){
                case Filtering:
                    guideLine.gl7();
                    gl_state = Gl_State.VisibilityControl;
                    break;
                case VisibilityControl:
                    guideLine.gl8_1();
                    gl_state = Gl_State.Adding1;
                    break;
                case Adding1:
                    guideLine.gl8_2();
                    gl_state = Gl_State.Adding2;
                    break;
                case Adding2:
                    guideLine.gl9();
                    gl_state = Gl_State.Editing;
                    break;
                case Editing:
                    guideLine.gl10_1();
                    gl_state = Gl_State.SAVING1;
                    break;
                case SAVING1:
                    guideLine.gl10_2();
                    gl_state = Gl_State.SAVING2;
                    break;
                case SAVING2:
                    guideLine.gl10_3();
                    gl_state = Gl_State.SAVING3;
                    break;
                case SAVING3:
                    guideLine.gl11();
                    pf.setFirstTimeLaunch2(false);
                    break;
            }
        }
        return true;
    }


    @Override
    public void onBackPressed() {
            if(isAlready ==true){
                nowellipse = null;
                ellipses.remove(selected_index);
                tempellipse.istoggled = true;
                correctionmode =false;
                isAlready = false;
                correctionbutton.setText("ADD");
                delbutton.setText("DONE");
                seekBar2.setVisibility(View.INVISIBLE);
                Toast.makeText(this, "원상 복구 되었습니다.", Toast.LENGTH_SHORT).show();
            }else if (drawerLayout.isDrawerOpen(Gravity.RIGHT)) {
                drawerLayout.closeDrawer(Gravity.RIGHT);
                tagFirstPressed = false;
            }
            //1번째 백버튼 클릭
            else if(System.currentTimeMillis()>backKeyPressedTime+2000){
                backKeyPressedTime = System.currentTimeMillis();
                Toast.makeText(this, "한번 더 눌러 메인 화면으로 이동", Toast.LENGTH_SHORT).show();
            }
            //2번째 백버튼 클릭 (종료)
            else{
                Intent intent = new Intent(this, StartScreen.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
    }




}
class GroupInfo{
    String location;
    String human;
    String spices;
    String address;
    int maxnum;
}
