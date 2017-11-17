package org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.MapsActivity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;
import org.reapbenefit.gautam.intern.potholedetectorbeta.TripListAdapter;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TriplistFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TriplistFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TriplistFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private ListView l;
    private ImageButton refreshButton;

    private ArrayList<Trip> trips = new ArrayList<>();

    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseDatabase database = FirebaseDatabase.getInstance();;
    private DatabaseReference myRef = database.getReference();
    private DatabaseReference profileRef = database.getReference();

    private long timeScore = 0;  // calculated based on time logged
    private long distanceScore = 0;  // calculated based on distance logged
    private CustomTripComparator comparator;
    ApplicationClass app;


    public TriplistFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TriplistFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TriplistFragment newInstance(String param1, String param2) {
        TriplistFragment fragment = new TriplistFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        app = ApplicationClass.getInstance();
        comparator = new CustomTripComparator();
        try {   // thrown when the user is not signed in
            myRef = myRef.child(mAuth.getCurrentUser().getUid());
            profileRef = profileRef.child("profiles").child(mAuth.getCurrentUser().getUid());

            myRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    timeScore = distanceScore = 0;
                    trips = new ArrayList<Trip>();

                    try {
                        for (DataSnapshot d : dataSnapshot.getChildren()) {
                            if (!d.getKey().contains("profile")) {
                                Trip t = d.getValue(Trip.class);
                                trips.add(t);
                                Log.d("SCORE", String.valueOf(t.getDuration()));
                                timeScore += t.getDuration();
                                distanceScore += t.getDistanceInKM();
                            }
                        }
                    }catch (DatabaseException e){

                    }
                    timeScore = (int) timeScore;
                    distanceScore = (int) distanceScore;
                    profileRef.child("timeScore").setValue(timeScore);
                    profileRef.child("distanceScore").setValue(distanceScore);
                    profileRef.child("totalTrips").setValue(trips.size());
                    // TODO : Optimize to use lesser db reads by storing locally and updating
                    createListView();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }catch (NullPointerException e){

        }
    }



    private long getTime(String date) throws ParseException{

        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
        Date d = sdf.parse(date);
        Log.d("Times", String.valueOf(d.getTime())+d.toString());
        return d.getTime();

    }

    private boolean internetAvailable(){
        ConnectivityManager connMgr = (ConnectivityManager)
                getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean isWifiConn = networkInfo.isConnected();
        networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        boolean isMobileConn = networkInfo.isConnected();
        if(isMobileConn || isWifiConn)
            return true;
        else
            return false;
    }

    private void createListView(){
        if(!trips.isEmpty() && getActivity()!=null) {
            Collections.sort(trips, new CustomTripComparator());
            l.setAdapter(new TripListAdapter(getActivity(), trips));
            l.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Trip t = trips.get(position);
                    File file = new File(getActivity().getApplicationContext().getFilesDir(), "analysis/" + t.getTrip_id() + ".csv");
                    if(!app.isTripInProgress() && file.exists()){ // check if file of same name is available in the analytics folder
                        Intent i = new Intent(getActivity(), MapsActivity.class);
                        i.putExtra("trip", t);
                        startActivity(i);
                    }else{
                        //Toast.makeText(getActivity(), )
                    }
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_triplist, container, false);
        refreshButton = (ImageButton) v.findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createListView();
                if(internetAvailable() && !trips.isEmpty())
                    Toast.makeText(getActivity(), "Refreshed", Toast.LENGTH_SHORT).show();
                else if(trips.isEmpty())
                    Toast.makeText(getActivity(), "Refreshing", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(getActivity(), "Internet not available, try later", Toast.LENGTH_SHORT).show();

            }
        });


        l = (ListView) v.findViewById(R.id.trips_list);
        createListView();
        l.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {



                /*   *********  Opens a map activity, DUMMY
                Intent intent = new Intent(getActivity(), MapsActivity.class);
                Bundle b = new Bundle();
                b.putSerializable("file", logfiles[i]);
                intent.putExtra("file", logfiles[i]);
                startActivity(intent);
                */
                // Start a map activity that plots the locations

                // Upload the related file to google drive
               /*
                String your_file_path = logfiles[i].getPath();
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + your_file_path));
                startActivity(Intent.createChooser(intent, ""));
                */

                // Open a detailed activity with a maps fragment
            }
        });
        return v;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public class CustomTripComparator implements Comparator<Trip> {
        @Override
        public int compare(Trip o2, Trip o1) {
            try{
                return Long.valueOf(getTime(o1.getEndTime())).compareTo(Long.valueOf(getTime(o2.getEndTime())));
            }catch (ParseException e){
                Log.d("Times", e.getMessage());
                return 0;
            }
        }
    }
}
