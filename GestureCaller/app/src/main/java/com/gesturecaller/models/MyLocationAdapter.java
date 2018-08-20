package com.gesturecaller.models;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.gesturecaller.R;
import com.gesturecaller.utils.Sv;

import java.util.List;

/**
 * Adapter template created by Ashish Mahla on 14/2/2017.
 */

@SuppressWarnings("FieldCanBeLocal")
public class MyLocationAdapter extends RecyclerView.Adapter<MyLocationAdapter.MyViewHolder> {

    private MyLocationClickListener MyLocationClickListener;
    private List<MyLocation> MyLocationList;

    public MyLocationAdapter(List<MyLocation> MyLocationList, MyLocationClickListener MyLocationClickListener) {
        this.MyLocationList = MyLocationList;
        this.MyLocationClickListener = MyLocationClickListener;
        setHasStableIds(true);
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.model_location, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        final MyLocation myLocation = MyLocationList.get(position);

        holder.name.setText(myLocation.getName());
        holder.locationCords.setText(myLocation.getStringLocation());
        holder.message.setText(myLocation.getMessage());
        holder.isEnabled.setChecked(myLocation.isEnabled());
        holder.distance.setText(myLocation.getFormattedDistance(holder.distance.getContext()));

        if (myLocation.isActive(holder.distance.getContext())) {
            holder.distance.setTextColor(Color.parseColor("#d50000"));
        }

        holder.isEnabled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean success = Sv.setLocationEnabled(holder.isEnabled.getContext(), myLocation.getId(), !myLocation.isEnabled());
                if (success) {
                    holder.isEnabled.setChecked(!myLocation.isEnabled());
                    myLocation.setEnabled(!myLocation.isEnabled());
                }
            }
        });
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return MyLocationList.size();
    }

    public interface MyLocationClickListener {
        void onMyLocationClickListener(View view, int position);

        void onMyLocationLongClickListener(View view, int position);
    }

    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        TextView name, locationCords, message, distance;
        CheckBox isEnabled;

        MyViewHolder(final View view) {
            super(view);

            //--------------------------------------------------------
            name = view.findViewById(R.id.tv_ml_name);
            locationCords = view.findViewById(R.id.tv_ml_location);
            message = view.findViewById(R.id.tv_ml_message);
            distance = view.findViewById(R.id.tv_ml_distance);

            isEnabled = view.findViewById(R.id.cb_ml_enabled);
            //--------------------------------------------------------

            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            MyLocationClickListener.onMyLocationClickListener(view, getLayoutPosition());
        }

        @Override
        public boolean onLongClick(View view) {
            MyLocationClickListener.onMyLocationLongClickListener(view, getLayoutPosition());
            return true;
        }
    }
}