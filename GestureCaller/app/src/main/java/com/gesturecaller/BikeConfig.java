package com.gesturecaller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.gesturecaller.models.ExceptionContact;
import com.gesturecaller.models.ExceptionContactAdapter;
import com.gesturecaller.utils.Sv;

import java.util.ArrayList;

/**
 * +Created by Ashish on 2/14/2018.
 */

public class BikeConfig extends Fragment implements ExceptionContactAdapter.ExceptionContactClickListener {

    private static final int PICK_CONTACT = 9021;
    ExceptionContactAdapter mAdapter;
    ArrayList<ExceptionContact> exceptionContactList = new ArrayList<>();
    RecyclerView recyclerView;
    Button saveMsg, addMore;
    TextView counter;
    TextInputEditText msg;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parent = inflater.inflate(R.layout.activity_bike_config, container, false);

        // initialize basic views of bike mode fragment
        initViews(parent);
        // initialize recycler view that shows exceptional contacts
        initRecyclerView(getActivity(), parent, R.id.rv_abm_exceptions);

        return parent;
    }

    private void initViews(View parent) {
        // setup "Bike Mode Activation" check box
        CheckBox bikeMode = parent.findViewById(R.id.set_bike_mode);
        // fetch current setting from database and set that to show
        bikeMode.setChecked(Sv.getBooleanSetting(Sv.BIKE_MODE, false));
        // setup action when bike mode checkbox is changed
        bikeMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Sv.setBooleanSetting(getActivity(), Sv.BIKE_MODE, b);
            }
        });

        // find other views as well
        saveMsg = parent.findViewById(R.id.b_abm_save_msg);
        addMore = parent.findViewById(R.id.b_abm_add_more);
        counter = parent.findViewById(R.id.tv_abm_counter);
        msg = parent.findViewById(R.id.tiet_abm_msg);
        msg.setText(Sv.getSetting(Sv.BIKE_MODE_MSG, ""));

        // setup action for "Save Message" on click
        saveMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // get msg string
                String sMsg = msg.getText().toString();
                // save new message to database
                Sv.setSetting(getActivity(), Sv.BIKE_MODE_MSG, sMsg);
                if (sMsg.isEmpty()) {
                    Snackbar.make(msg, "Message updated (Do not send message)", Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(msg, "Message updated", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        // setup action on "Add More Exceptional Contacts" button on click
        addMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addMore();
            }
        });
    }

    private void addMore() {
        // start contacts activity to pick more contacts to add for exceptions
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT);
        // after activity launched, wait for its result in onActivityResult method

        /*ExceptionContact ec = new ExceptionContact();
        ec.setContactName("Maa");
        ec.setContact("+919414787140");*/
    }

    private void refreshList() {
        // set refresh list to clear list and add all exceptional contacts again
        if (mAdapter != null) {
            exceptionContactList.clear();
            exceptionContactList.addAll(Sv.getAllExceptionalContacts(getActivity()));
            mAdapter.notifyDataSetChanged();
        }
        // setup counter of contacts count
        if (counter != null) {
            counter.setText(String.valueOf(exceptionContactList.size()));
        }
    }

    private void initRecyclerView(Context context, View parent, int rv_id) {
        // get recyclerView's view in layout
        recyclerView = parent.findViewById(rv_id);
        // create a layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(context);
        // setup layout manager to recycler view
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        // get all exceptional contacts from the database
        exceptionContactList = Sv.getAllExceptionalContacts(getActivity());
        // initialize adapter to manage inflation of the various models
        mAdapter = new ExceptionContactAdapter(exceptionContactList, this);
        // setup adapter on recycler view
        recyclerView.setAdapter(mAdapter);
        // refresh list
        refreshList();
    }

    @Override
    public void onExceptionContactClickListener(View view, int position) {
        // action when exceptional contact is clicked
        Sv.deleteExceptionalContactById(getActivity(), exceptionContactList.get(position).getId());
        refreshList();
    }

    @Override
    public void onExceptionContactLongClickListener(View view, final int position) {
        // action when exceptional contact is long clicked
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        // activity result is invoked when a contact is returned
        if (reqCode == PICK_CONTACT && resultCode == Activity.RESULT_OK) {
            Uri contactData = data.getData();
            // cursor containing all contacts that are selected
            Cursor c = getActivity().managedQuery(contactData, null, null, null, null);
            if (c.getCount() > 0) {
                // if cursor contain some contacts goto first contact
                c.moveToFirst();
                while (!c.isAfterLast()) {
                    // add exceptional contact to database when there are still contacts left in cursor
                    String id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                    String hasPhone = c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                    String cNumber = null;
                    if (hasPhone.equalsIgnoreCase("1")) {
                        Cursor phones = getActivity().getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                                null, null);
                        phones.moveToFirst();
                        cNumber = phones.getString(phones.getColumnIndex("data1"));
                        System.out.println("number is:" + cNumber);
                    }
                    String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    if (cNumber != null && !cNumber.isEmpty()) {
                        Sv.addExceptionalContact(getActivity(), new ExceptionContact(name, cNumber));
                    }
                    c.moveToNext();
                }

                // refresh list after everything is done
                refreshList();
            }
        }
    }
}