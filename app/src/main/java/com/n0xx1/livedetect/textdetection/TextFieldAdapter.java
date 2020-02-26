package com.n0xx1.livedetect.textdetection;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.n0xx1.livedetect.R;
import com.n0xx1.livedetect.textdetection.TextFieldAdapter.TextFieldViewHolder;

import java.util.List;

/** Presents a list of field info in the detected text. */
class TextFieldAdapter extends RecyclerView.Adapter<TextFieldViewHolder> {

    static class TextFieldViewHolder extends RecyclerView.ViewHolder {

        static TextFieldViewHolder create(ViewGroup parent) {
            View view =
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.text_field, parent, false);
            return new TextFieldViewHolder(view);
        }

        private final TextView labelView;
        private final TextView valueView;

        private TextFieldViewHolder(View view) {
            super(view);
            labelView = view.findViewById(R.id.text_field_label);
            valueView = view.findViewById(R.id.text_field_value);
        }

        void bindTextField(TextField textField) {
            labelView.setText(textField.label);
            valueView.setText(textField.value);
        }
    }

    private final List<TextField> textFieldList;

    TextFieldAdapter(List<TextField> textFieldList) {
        this.textFieldList = textFieldList;
    }

    @Override
    @NonNull
    public TextFieldViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return TextFieldViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull TextFieldViewHolder holder, int position) {
        holder.bindTextField(textFieldList.get(position));
    }

    @Override
    public int getItemCount() {
        return textFieldList.size();
    }
}
