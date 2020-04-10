package com.n0xx1.livedetect.staticdetection;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.n0xx1.livedetect.R;
import com.n0xx1.livedetect.staticdetection.LabelAdapter.TextViewHolder;
import com.n0xx1.livedetect.text2speech.Text2Speech;

import java.util.List;

/** Presents the list of label items from cloud label search. */
public class LabelAdapter extends RecyclerView.Adapter<TextViewHolder> {

    private Text2Speech tts;

    static class TextViewHolder extends RecyclerView.ViewHolder {

//        static TextViewHolder create(ViewGroup parent) {
//            View view =
//                    LayoutInflater.from(parent.getContext()).inflate(R.layout.label_item, parent, false);
//            return new TextViewHolder(view);
//        }

        private final ImageView imageView;
        private final TextView titleView;
        private final TextView subtitleView;
        private final int imageSize;

        private TextViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.label_image);
            titleView = view.findViewById(R.id.label_title);
            subtitleView = view.findViewById(R.id.label_subtitle);
            imageSize = view.getResources().getDimensionPixelOffset(R.dimen.product_item_image_size);
        }

        void bindLabel(Label label) {
//            imageView.setImageDrawable(null);
//            if (!TextUtils.isEmpty(label.imageUrl)) {
//                new ImageDownloadTask(imageView, imageSize).execute(label.imageUrl);
//            } else {
//                imageView.setImageResource(R.drawable.logo_google_cloud);
//            }
            titleView.setText(label.label);
//            subtitleView.setText(label.vertices.toString());
        }
    }

    private final List<Label> labelList;

    public LabelAdapter(List<Label> labelList) {
        this.labelList = labelList;
    }

    @Override
    @NonNull
    public TextViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext()).inflate(R.layout.label_item, parent, false);
        TextViewHolder holder= new LabelAdapter.TextViewHolder(view);


        tts = new Text2Speech(parent.getContext(), (Activity) parent.getContext());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tts.speech(labelList.get(holder.getAdapterPosition()).label);
            }
        });

//        return TextViewHolder.create(parent);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull TextViewHolder holder, int position) {
        holder.bindLabel(labelList.get(position));
    }

    @Override
    public int getItemCount() {
        return labelList.size();
    }
}
