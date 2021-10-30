package com.example.CS_IU_proto_1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class MyRecord extends AppCompatActivity {

    boolean isScroll = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_record);




        // 리사이클러뷰에 표시할 데이터 리스트 생성.
        ArrayList<Data> list = new ArrayList<>();

        //Data 넣기
        list.add(new Data("10월 8일", "name1", "수원시 영통구 어쩌고 저쩌고 어쩌고 저쩌고 어쩌고 저쩌고 어쩌고 저쩌고 어쩌고 저쩌고"));
        list.add(new Data("10월 9일", "name2", "수원시 영통구 어쩌고 저쩌고"));
        list.add(new Data("10월 10일", "name3", "수원시 영통구 어쩌고 저쩌고"));
        list.add(new Data("10월 11일", "name4", "수원시 영통구 어쩌고 저쩌고"));
        list.add(new Data("10월 12일", "name5", "수원시 영통구 어쩌고 저쩌고"));
        list.add(new Data("10월 13일", "name6", "수원시 영통구 어쩌고 저쩌고"));
        list.add(new Data("10월 14일", "name7", "수원시 영통구 어쩌고 저쩌고"));
        list.add(new Data("10월 15일", "name8", "수원시 영통구 어쩌고 저쩌고"));

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
                    int position = recyclerView.getChildAdapterPosition(child);

                    //여기에서 리스트 안에서 몇번째 아이템 선택됐는지 알 수 있음
                    if(!isScroll) {
                        Log.d("테스트", "Position: " + position);
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
            holder.comp1.setText(mData.get(position).date);
            holder.comp2.setText(mData.get(position).filename);
            holder.comp3.setText(mData.get(position).addr);
        }

        // getItemCount() - 전체 데이터 갯수 리턴.
        @Override
        public int getItemCount() {
            return mData.size() ;
        }
    }
}