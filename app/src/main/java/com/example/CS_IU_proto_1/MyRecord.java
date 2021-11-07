package com.example.CS_IU_proto_1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Objects;

import javax.xml.transform.Result;

public class MyRecord extends AppCompatActivity {

    boolean isScroll = false;
    int popupMode = 1;
    int selected = -1;

    ConstraintLayout popupLayout;
    Button btnOk, btnCancel;
    ImageButton btnDelete;
    TextView popupText, noFileText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_record);


        popupLayout = findViewById(R.id.popUpLayout);
        btnOk = findViewById(R.id.btnOpen);
        btnCancel = findViewById(R.id.btnCancel);
        btnDelete = findViewById(R.id.btnDelete);
        popupText = findViewById(R.id.txtPopUpText);
        noFileText = findViewById(R.id.txtNoFile);


        File mydir = this.getFilesDir();
        File[] listFiles = mydir.listFiles();
        Gson gson = new Gson();
        ArrayList<JsonObject> jsonObjects = new ArrayList<JsonObject>();
        // 리사이클러뷰에 표시할 데이터 리스트 생성.
        ArrayList<Data> list = new ArrayList<>();
        ArrayList<Integer> order = new ArrayList<>();

        for (int i = 1; i< Objects.requireNonNull(listFiles).length; i++){
            String filename = listFiles[i].getName();
            if(filename.substring(filename.lastIndexOf(".")+1,filename.length()).equals("json")){
                try {
                    JsonReader jsonReader = new JsonReader(new InputStreamReader(openFileInput(filename),"UTF-8"));
                    jsonReader.setLenient(true);
                    JsonObject tempjson = gson.fromJson(jsonReader,JsonObject.class);


                    list.add(new Data(tempjson.get("space").getAsString(),filename.substring(0,filename.lastIndexOf(".")),tempjson.get("human").getAsString()));
                    order.add(i);
                    jsonObjects.add(tempjson);

                } catch (FileNotFoundException | UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                Log.i("i th", ""+i);

                // "i + 1"
            }

        }

        //파일 없으면 파일 없다고 띄우기
        if(list.size() == 0)
            noFileText.setVisibility(View.VISIBLE);

        // 리사이클러뷰에 LinearLayoutManager 객체 지정.
        RecyclerView recyclerView = findViewById(R.id.recyclerView) ;
        recyclerView.setLayoutManager(new LinearLayoutManager(this)) ;

        // 리사이클러뷰에 SimpleTextAdapter 객체 지정.
        CustomAdapter adapter = new CustomAdapter(list) ;
        recyclerView.setAdapter(adapter) ;

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if(newState == RecyclerView.SCROLL_STATE_DRAGGING)
                    isScroll = true;
                else
                    isScroll = false;
            }
        });

        //각 아이템 선택 리스너
        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {

                if(e.getAction() == MotionEvent.ACTION_UP) {
                    View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    int pos = recyclerView.getChildAdapterPosition(child);

                    //여기에서 리스트 안에서 몇번째 아이템 선택됐는지 알 수 있음
                    if(!isScroll && popupLayout.getVisibility() != View.VISIBLE) {
                        if(pos != -1){
                            selected = pos;
                            popupMode = 1;
                            btnOk.setText("열기");
                            btnDelete.setVisibility(View.VISIBLE);
                            popupText.setText(list.get(selected).filename + " 을 여시겠습니까?");
                            popupLayout.setVisibility(View.VISIBLE);
                        }
                    }
                }
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });

        //삭제 창으로 변환
        btnDelete.setOnClickListener(l -> {
            popupMode = 2;
            popupText.setText(list.get(selected).filename + " 을 삭제하시겠습니까?");
            btnOk.setText("삭제");
            btnDelete.setVisibility(View.GONE);
        });

        //취소
        btnCancel.setOnClickListener(l -> {
            popupLayout.setVisibility(View.GONE);
        });

        //파일 열기 또는 파일 삭제
        btnOk.setOnClickListener(l -> {
            //파일 열기
            if(popupMode == 1){

                /*
                파일 로드


                 */

                JsonObject nowobject = jsonObjects.get(selected);
                Type type = new TypeToken<ArrayList<Ellipse>>() {}.getType();
                Plane plane = gson.fromJson(nowobject.getAsJsonObject("plane"),Plane.class);
                ArrayList<Ellipse> ellipses = gson.fromJson(nowobject.getAsJsonArray("ellipses"),type);
                float[] projMX = gson.fromJson(nowobject.getAsJsonArray("projMX"),float[].class);
                float[] viewMX = gson.fromJson(nowobject.getAsJsonArray("viewMX"),float[].class);
                float[] cameratrans = gson.fromJson(nowobject.getAsJsonArray("cameratrans"),float[].class);
                float offset = nowobject.get("offset").getAsFloat();
                String speice = nowobject.get("speice").getAsString();
                String date = nowobject.get("date").getAsString();
                String location = nowobject.get("location").getAsString();
                String address = nowobject.get("space").getAsString();
                float longivity = nowobject.get("long").getAsFloat();
                String human = nowobject.get("human").getAsString();
                String filename = list.get(selected).filename;

                Intent intent = new Intent(MyRecord.this, ResultActivity.class);
                intent.putExtra("from",2);
                intent.putParcelableArrayListExtra("Ellipse",ellipses);
                intent.putExtra("plane",plane);
                intent.putExtra("projMat",projMX);
                intent.putExtra("viewMat",viewMX);
                intent.putExtra("cameratrans",cameratrans);
                intent.putExtra("offset",offset);


                try {
                    InputStream inputStream = openFileInput(filename+".JPG");
                    Bitmap image = BitmapFactory.decodeStream(inputStream);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    if((listFiles[order.get(selected)+1].length()/1024)>=500 ){

                        image.compress(Bitmap.CompressFormat.JPEG, 30,stream);
                    }else{

                        image.compress(Bitmap.CompressFormat.JPEG, 100,stream);
                    }
                    byte[] bytes = stream.toByteArray();
                    intent.putExtra("image",bytes);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                intent.putExtra("speices",speice);
                intent.putExtra("date",date);
                intent.putExtra("location",location);
                intent.putExtra("space",address);
                intent.putExtra("long",longivity);
                intent.putExtra("filename",filename);
                intent.putExtra("human",human);
                startActivity(intent);
                //bitmap 열고 압축해서 다시 주기



                // make intent!!

                Toast.makeText(this, "파일이 성공적으로 열렸습니다.", Toast.LENGTH_SHORT).show();
            }
            //파일 삭제
            else{
                boolean isDeleted = listFiles[order.get(selected)].delete();


                if(isDeleted) {
                    listFiles[order.get(selected)+1].delete();
                    jsonObjects.remove(selected);
                    list.remove(selected);
                    adapter.notifyDataSetChanged();
                    //파일 없으면 파일 없다고 띄우기
                    if (list.size() == 0)
                        noFileText.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(this, "삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }
            popupLayout.setVisibility(View.GONE);
        });

    }

    class Data{
        String date;
        String filename;
        String addr;

        Data(String _date, String _filename, String _addr){
            date = _date;
            filename = _filename;
            addr = _addr;
        }
    }

    private class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

        private ArrayList<Data> mData = null ;

        // 아이템 뷰를 저장하는 뷰홀더 클래스.
        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView comp1, comp2, comp3;

            ViewHolder(View itemView) {
                super(itemView) ;

                // 뷰 객체에 대한 참조. (hold strong reference)
                comp1 = itemView.findViewById(R.id.component1) ;
                comp2 = itemView.findViewById(R.id.component2) ;
                comp3 = itemView.findViewById(R.id.component3) ;
            }
        }

        // 생성자에서 데이터 리스트 객체를 전달받음.
        CustomAdapter(ArrayList<Data> list) {
            mData = list ;
        }

        // onCreateViewHolder() - 아이템 뷰를 위한 뷰홀더 객체 생성하여 리턴.
        @Override
        public CustomAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext() ;
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) ;

            View view = inflater.inflate(R.layout.recyclerview_item, parent, false) ;
            CustomAdapter.ViewHolder vh = new CustomAdapter.ViewHolder(view) ;

            return vh ;
        }

        // onBindViewHolder() - position에 해당하는 데이터를 뷰홀더의 아이템뷰에 표시.
        @Override
        public void onBindViewHolder(CustomAdapter.ViewHolder holder, int position) {
            holder.comp2.setText(mData.get(position).date);
            holder.comp1.setText(mData.get(position).filename);
            holder.comp3.setText(mData.get(position).addr);
        }

        // getItemCount() - 전체 데이터 갯수 리턴.
        @Override
        public int getItemCount() {
            return mData.size() ;
        }
    }
}