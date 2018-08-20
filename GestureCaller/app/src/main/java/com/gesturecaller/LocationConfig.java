package com.gesturecaller;

import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
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
import android.widget.Toast;

import com.gesturecaller.models.MyLocation;
import com.gesturecaller.models.MyLocationAdapter;
import com.gesturecaller.utils.GPSTracker;
import com.gesturecaller.utils.Sv;

import java.util.ArrayList;

/**
 * +Created by Ashish on 2/14/2018.
 */

public class LocationConfig extends Fragment implements MyLocationAdapter.MyLocationClickListener {

    GPSTracker gps;
    TextView latitude, longitude;
    FloatingActionButton refresh_loc;
    MyLocationAdapter mAdapter;
    ArrayList<MyLocation> myLocationList = new ArrayList<>();
    RecyclerView recyclerView;
    TextView counter;
    Button addCurrent;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parent = inflater.inflate(R.layout.activity_location_config, container, false);
        if (gps == null) gps = new GPSTracker(getActivity());

        initViews(parent);
        refreshLocation();
        initRecyclerView(getActivity(), parent, R.id.rv_alc_gps_locations);

        return parent;
    }

    private void initViews(View parent) {
        // setup location mode views
        CheckBox locationMode = parent.findViewById(R.id.set_location_mode);
        // initialize location mode activation checkbox from database
        locationMode.setChecked(Sv.getBooleanSetting(Sv.LOCATION_MODE, false));
        locationMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                // on location check change, reflect it to database
                Sv.setBooleanSetting(getActivity(), Sv.LOCATION_MODE, b);
            }
        });

        // other views like latitude and longitude text views are also initialized
        latitude = parent.findViewById(R.id.tv_alc_latitude);
        longitude = parent.findViewById(R.id.tv_alc_longitude);

        refresh_loc = parent.findViewById(R.id.fab_alc_refresh_location);
        refresh_loc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshLocation();
            }
        });

        counter = parent.findViewById(R.id.tv_alc_counter);

        addCurrent = parent.findViewById(R.id.b_alc_add_current);
        // add current location button on click listener
        addCurrent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Location currLoc = gps.getLocation();
                if (currLoc == null) {
                    // if currLoc is inaccessible, show warning
                    Toast.makeText(getActivity(), "Unable to read location.", Toast.LENGTH_SHORT).show();
                    return;
                }

                LayoutInflater inflater = LayoutInflater.from(getActivity());
                View addLocView = inflater.inflate(R.layout.dialog_add_location, null);

                final TextInputLayout nameP = addLocView.findViewById(R.id.til_dal_name);
                final TextInputLayout msgP = addLocView.findViewById(R.id.til_dal_msg);

                final TextInputEditText name = addLocView.findViewById(R.id.tiet_dal_name);
                final TextInputEditText msg = addLocView.findViewById(R.id.tiet_dal_msg);

                // create a dialog box for location name and message input
                AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
                adb.setView(addLocView);
                adb.setPositiveButton("Save", null);

                final AlertDialog dialog = adb.create();
                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialogInterface) {
                        Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                boolean nameValid = !name.getText().toString().isEmpty();
                                boolean msgValid = !msg.getText().toString().isEmpty();

                                if (!nameValid) {
                                    nameP.setError("Nickname required");
                                }

                                if (nameValid) {
                                    MyLocation location = new MyLocation();
                                    location.setName(name.getText().toString());
                                    location.setMessage(msg.getText().toString());
                                    location.setEnabled(true);
                                    location.setLatitude(currLoc.getLatitude());
                                    location.setLongitude(currLoc.getLongitude());

                                    Sv.addLocation(getActivity(), location);
                                    dialog.dismiss();
                                    refreshList();
                                }
                            }
                        });
                    }
                });
                dialog.show();
            }
        });
    }

    private void refreshLocation() {
        // refresh location if gps is accessible
        if (gps != null) {
            if (gps.canGetLocation()) {
                Location location = gps.getLocation();

                if (location != null) {
                    latitude.setText(String.valueOf(location.getLatitude()));
                    longitude.setText(String.valueOf(location.getLongitude()));
                } else {
                    latitude.setText("---");
                    longitude.setText("---");
                    Toast.makeText(getActivity(), "Unable to access location.", Toast.LENGTH_SHORT).show();
                }
            } else {
                // can't get location
                // GPS or Network is not enabled
                // Ask user to enable GPS/network in settings
                gps.showSettingsAlert();
            }
        }
    }

    private void initRecyclerView(Context context, View parent, int rv_id) {
        // added location's list initialization
        recyclerView = parent.findViewById(rv_id);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        mAdapter = new MyLocationAdapter(myLocationList, this);
        recyclerView.setAdapter(mAdapter);

        refreshList();
    }

    private void refreshList() {
        // refresh locations list
        if (mAdapter != null) {
            myLocationList.clear();
            myLocationList.addAll(Sv.getAllLocations(getActivity()));
            mAdapter.notifyDataSetChanged();
        }
        if (counter != null) {
            counter.setText(String.valueOf(myLocationList.size()));
        }
    }

    @Override
    public void onMyLocationClickListener(View view, int position) {
        // on location item click action
    }

    @Override
    public void onMyLocationLongClickListener(View view, final int position) {
        // on location item long click delete the item
        Sv.deleteLocationById(view.getContext(), myLocationList.get(position).getId());
        refreshList();
    }

    @Override
    public void onResume() {
        super.onResume();
        // setup gps tracker on fragment resume
        if (gps == null) gps = new GPSTracker(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();
        // remove gps if it is available on fragment pause
        if (gps != null) gps.stopUsingGPS();
    }
}