package com.n0xx1.livedetect.textdetection;

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

/** Displays the bottom sheet to present text fields contained in the detected text. */
public class TextResultFragment extends BottomSheetDialogFragment {

    private static final String TAG = "TextResultFragment";
    private static final String ARG_TEXT_FIELD_LIST = "arg_text_field_list";

    public static void show(
            FragmentManager fragmentManager, ArrayList<TextField> textFieldArrayList) {
        TextResultFragment textResultFragment = new TextResultFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(ARG_TEXT_FIELD_LIST, textFieldArrayList);
        textResultFragment.setArguments(bundle);
        textResultFragment.show(fragmentManager, TAG);
    }

    public static void dismiss(FragmentManager fragmentManager) {
        TextResultFragment textResultFragment =
                (TextResultFragment) fragmentManager.findFragmentByTag(TAG);
        if (textResultFragment != null) {
            textResultFragment.dismiss();
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater layoutInflater,
            @Nullable ViewGroup viewGroup,
            @Nullable Bundle bundle) {
        View view = layoutInflater.inflate(R.layout.text_bottom_sheet, viewGroup);
        ArrayList<TextField> textFieldList;
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(ARG_TEXT_FIELD_LIST)) {
            textFieldList = arguments.getParcelableArrayList(ARG_TEXT_FIELD_LIST);
        } else {
            Log.e(TAG, "No text field list passed in!");
            textFieldList = new ArrayList<>();
        }

        RecyclerView fieldRecyclerView = view.findViewById(R.id.text_field_recycler_view);
        fieldRecyclerView.setHasFixedSize(true);
        fieldRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        fieldRecyclerView.setAdapter(new TextFieldAdapter(textFieldList));

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