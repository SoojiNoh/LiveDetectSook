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
import com.n0xx1.livedetect.staticdetection.TextAdapter.TextViewHolder;
import com.n0xx1.livedetect.text2speech.Text2Speech;

import java.util.List;

/** Presents the list of text items from cloud text search. */
public class TextAdapter extends RecyclerView.Adapter<TextViewHolder> {

    private Text2Speech tts;

    static class TextViewHolder extends RecyclerView.ViewHolder {

//        static TextViewHolder create(ViewGroup parent) {
//            View view =
//                    LayoutInflater.from(parent.getContext()).inflate(R.layout.text_item, parent, false);
//            return new TextViewHolder(view);
//        }

        private final ImageView imageView;
        private final TextView titleView;
        private final TextView subtitleView;
        private final int imageSize;

        private TextViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.text_image);
            titleView = view.findViewById(R.id.text_title);
            subtitleView = view.findViewById(R.id.text_subtitle);
            imageSize = view.getResources().getDimensionPixelOffset(R.dimen.product_item_image_size);
        }

        void bindText(Text text) {
//            imageView.setImageDrawable(null);
//            if (!TextUtils.isEmpty(text.imageUrl)) {
//                new ImageDownloadTask(imageView, imageSize).execute(text.imageUrl);
//            } else {
//                imageView.setImageResource(R.drawable.logo_google_cloud);
//            }
            titleView.setText(text.text);
            subtitleView.setText(text.vertices.toString());
        }
    }

    private final List<Text> textList;

    public TextAdapter(List<Text> textList) {
        this.textList = textList;
    }

    @Override
    @NonNull
    public TextViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext()).inflate(R.layout.text_item, parent, false);
        TextViewHolder holder= new TextAdapter.TextViewHolder(view);


        tts = new Text2Speech(parent.getContext(), (Activity) parent.getContext());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tts.speech(textList.get(holder.getAdapterPosition()).text);
            }
        });

//        return TextViewHolder.create(parent);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull TextViewHolder holder, int position) {
        holder.bindText(textList.get(position));
    }

    @Override
    public int getItemCount() {
        return textList.size();
    }
}
