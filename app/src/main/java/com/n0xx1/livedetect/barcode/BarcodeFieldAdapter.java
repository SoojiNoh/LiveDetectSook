package com.n0xx1.livedetect.barcode;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.n0xx1.livedetect.MainActivity;
import com.n0xx1.livedetect.R;
import com.n0xx1.livedetect.barcode.BarcodeFieldAdapter.BarcodeFieldViewHolder;

import java.util.List;

/** Presents a list of field info in the detected barcode. */
class BarcodeFieldAdapter extends RecyclerView.Adapter<BarcodeFieldViewHolder> {

    private static final String TAG = "BarcodeFieldAdapter";

    static class BarcodeFieldViewHolder extends RecyclerView.ViewHolder {

        static BarcodeFieldViewHolder create(ViewGroup parent, Context context) {
            View view =
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.barcode_field, parent, false);

            view.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View v) {
                    Log.d(TAG, "*****BarcodeFieldAdpaterClicked");
                    MainActivity mainActivity = (MainActivity)context;
                    mainActivity.productCrawlEngine.crawl();
                }
            });
            return new BarcodeFieldViewHolder(view);
        }

        private final TextView labelView;
        private final TextView valueView;

        private BarcodeFieldViewHolder(View view) {
            super(view);
            labelView = view.findViewById(R.id.barcode_field_label);
            valueView = view.findViewById(R.id.barcode_field_value);
        }

        void bindBarcodeField(BarcodeField barcodeField) {
            labelView.setText(barcodeField.label);
            valueView.setText(barcodeField.value);
        }
    }

    private final List<BarcodeField> barcodeFieldList;
    public Context context;

    BarcodeFieldAdapter(List<BarcodeField> barcodeFieldList, Context context) {
        this.barcodeFieldList = barcodeFieldList;
        this.context = context;
    }

    @Override
    @NonNull
    public BarcodeFieldViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return BarcodeFieldViewHolder.create(parent, context);
    }

    @Override
    public void onBindViewHolder(@NonNull BarcodeFieldViewHolder holder, int position) {
        holder.bindBarcodeField(barcodeFieldList.get(position));
    }

    @Override
    public int getItemCount() {
        return barcodeFieldList.size();
    }

}
