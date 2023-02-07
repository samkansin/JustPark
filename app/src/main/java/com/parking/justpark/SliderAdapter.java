package com.parking.justpark;




import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

public class SliderAdapter extends PagerAdapter{

    Context context;
    LayoutInflater layoutInflater;
    static int state = 1;
    public SliderAdapter(Context context){
        this.context = context;
    }

    int images[] = {
            R.drawable.image1,
            R.drawable.image2,
            R.drawable.image3
    };

    int titles[] = {
            R.string.first_title,
            R.string.second_title,
            R.string.third_title
    };

    int descriptions[] = {
            R.string.first_desc,
            R.string.second_desc,
            R.string.third_desc
    };


    @Override
    public int getCount() {
        return titles.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == (LinearLayout) object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        layoutInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.slide_layout, container, false);
        state++;

            ImageView imageView = view.findViewById(R.id.slider_img);
            TextView title = view.findViewById(R.id.slider_title);
            TextView desc = view.findViewById(R.id.slider_desc);

            imageView.setImageResource(images[position]);
            title.setText(titles[position]);
            desc.setText(descriptions[position]);

            container.addView(view);

        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((LinearLayout) object);
    }
}
