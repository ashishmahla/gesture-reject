package com.gesturecaller.models;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gesturecaller.R;

import java.util.List;

/**
 * Adapter template created by Ashish Mahla on 14/2/2017.
 */

@SuppressWarnings("FieldCanBeLocal")
public class ExceptionContactAdapter extends RecyclerView.Adapter<ExceptionContactAdapter.MyViewHolder> {

    private ExceptionContactClickListener ExceptionContactClickListener;
    private List<ExceptionContact> ExceptionContactList;

    public ExceptionContactAdapter(List<ExceptionContact> ExceptionContactList, ExceptionContactClickListener ExceptionContactClickListener) {
        this.ExceptionContactList = ExceptionContactList;
        this.ExceptionContactClickListener = ExceptionContactClickListener;
        setHasStableIds(true);
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.model_exception_contact, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        final ExceptionContact ec = ExceptionContactList.get(position);

        holder.name.setText(ec.getContactName());
        holder.contact.setText(ec.getContact());
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
        return ExceptionContactList.size();
    }

    public interface ExceptionContactClickListener {
        void onExceptionContactClickListener(View view, int position);

        void onExceptionContactLongClickListener(View view, int position);
    }

    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        TextView name, contact;
        ImageView delete;

        MyViewHolder(final View view) {
            super(view);

            //--------------------------------------------------------
            name = view.findViewById(R.id.tv_mec_name);
            contact = view.findViewById(R.id.tv_mec_contact);
            delete = view.findViewById(R.id.iv_mec_delete);
            //--------------------------------------------------------

            delete.setOnClickListener(this);
            view.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            ExceptionContactClickListener.onExceptionContactClickListener(v, getLayoutPosition());
        }

        @Override
        public boolean onLongClick(View view) {
            ExceptionContactClickListener.onExceptionContactLongClickListener(view, getLayoutPosition());
            return false;
        }
    }
}