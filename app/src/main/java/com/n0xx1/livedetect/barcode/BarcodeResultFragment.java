package com.n0xx1.livedetect.barcode;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.n0xx1.livedetect.R;
import com.n0xx1.livedetect.camera.WorkflowModel;
import com.n0xx1.livedetect.camera.WorkflowModel.WorkflowState;

import java.util.ArrayList;

/** Displays the bottom sheet to present barcode fields contained in the detected barcode. */
public class BarcodeResultFragment extends BottomSheetDialogFragment{

    private static final String TAG = "BarcodeResultFragment";
    private static final String ARG_BARCODE_FIELD_LIST = "arg_barcode_field_list";

    RecyclerView fieldRecyclerView;

    public static void show(
            FragmentManager fragmentManager, ArrayList<BarcodeField> barcodeFieldArrayList, ProductCrawlEngine productCrawlEngine) {
        BarcodeResultFragment barcodeResultFragment = new BarcodeResultFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(ARG_BARCODE_FIELD_LIST, barcodeFieldArrayList);
        barcodeResultFragment.setArguments(bundle);
        barcodeResultFragment.show(fragmentManager, TAG);
    }

    public static void dismiss(FragmentManager fragmentManager) {
        BarcodeResultFragment barcodeResultFragment =
                (BarcodeResultFragment) fragmentManager.findFragmentByTag(TAG);
        if (barcodeResultFragment != null) {
            barcodeResultFragment.dismiss();
        }
    }


    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater layoutInflater,
            @Nullable ViewGroup viewGroup,
            @Nullable Bundle bundle) {
        View view = layoutInflater.inflate(R.layout.barcode_bottom_sheet, viewGroup);
        ArrayList<BarcodeField> barcodeFieldList;
        ProductCrawlEngine productCrawlEngine;
        Bundle arguments = getArguments();

        if (arguments != null && arguments.containsKey(ARG_BARCODE_FIELD_LIST)) {
            barcodeFieldList = arguments.getParcelableArrayList(ARG_BARCODE_FIELD_LIST);
        } else {
            Log.e(TAG, "No barcode field list passed in!");
            barcodeFieldList = new ArrayList<>();
        }



        String strtext = getArguments().getString("edttext");

        fieldRecyclerView = view.findViewById(R.id.barcode_field_recycler_view);
        fieldRecyclerView.setHasFixedSize(true);
        fieldRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        fieldRecyclerView.setAdapter(new BarcodeFieldAdapter(barcodeFieldList, getActivity()));

        return view;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialogInterface) {
        if (getActivity() != null) {
            // Back to working state after the bottom sheet is dismissed.
            ViewModelProviders.of(getActivity())
                    .get(WorkflowModel.class)
                    .setWorkflowState(WorkflowState.DETECTING);
        }
        super.onDismiss(dialogInterface);
    }



}
