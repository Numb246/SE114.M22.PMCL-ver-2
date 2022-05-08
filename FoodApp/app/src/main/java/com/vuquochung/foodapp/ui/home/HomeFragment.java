package com.vuquochung.foodapp.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.asksira.loopingviewpager.LoopingViewPager;
import com.vuquochung.foodapp.Adapter.MyBestDealsAdapter;
import com.vuquochung.foodapp.Adapter.MyPopularCategoriesAdapter;
import com.vuquochung.foodapp.R;
import com.vuquochung.foodapp.databinding.FragmentHomeBinding;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    Unbinder unbinder;

    @BindView(R.id.recycler_popular)
    RecyclerView recycler_popular;
    @BindView(R.id.viewpager)
    LoopingViewPager viewPager;

    LayoutAnimationController layoutAnimationController;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        unbinder= ButterKnife.bind(this,root);
        init();
        homeViewModel.getPopularList().observe(getViewLifecycleOwner(),popularCategoryModels -> {
            MyPopularCategoriesAdapter adapter=new MyPopularCategoriesAdapter(getContext(),popularCategoryModels);
            recycler_popular.setAdapter(adapter);
            recycler_popular.setLayoutAnimation(layoutAnimationController);
        });
        homeViewModel.getBestDealList().observe(getViewLifecycleOwner(),bestDealModels -> {
            MyBestDealsAdapter adapter=new MyBestDealsAdapter(getContext(),bestDealModels,true);
            viewPager.setAdapter(adapter);
        });
        return root;
    }

    private void init() {
        layoutAnimationController= AnimationUtils.loadLayoutAnimation(getContext(),R.anim.layout_item_from_left);
        recycler_popular.setHasFixedSize(true);
        recycler_popular.setLayoutManager(new LinearLayoutManager(getContext(),RecyclerView.HORIZONTAL,false));

    }

    @Override
    public void onResume() {
        super.onResume();
        viewPager.resumeAutoScroll();
    }

    @Override
    public void onPause() {
        viewPager.pauseAutoScroll();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}