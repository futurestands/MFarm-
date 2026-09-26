package dev.mfarm.com.mfarm.fragments;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import dev.mfarm.com.mfarm.R;

/**
 * Created by Methu on 4/19/2016.
 */
public class MedicationFragment extends Fragment {
    public MedicationFragment newInstance(){
        MedicationFragment _MedicationFragment = new MedicationFragment();

        return _MedicationFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_medication, container, false);

        return view;
    }
}
