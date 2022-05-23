package com.vuquochung.foodapp.ui.view_orders;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviderKt;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vuquochung.foodapp.Adapter.MyCommentAdapter;
import com.vuquochung.foodapp.Adapter.MyFoodListAdapter;
import com.vuquochung.foodapp.Adapter.MyOrdersAdapter;
import com.vuquochung.foodapp.Callback.ICommentCallbackListener;
import com.vuquochung.foodapp.Callback.ILoadOrderCallbackListener;
import com.vuquochung.foodapp.Common.Common;
import com.vuquochung.foodapp.Model.CommentModel;
import com.vuquochung.foodapp.Model.FoodModel;
import com.vuquochung.foodapp.Model.Order;
import com.vuquochung.foodapp.R;
import com.vuquochung.foodapp.ui.comments.CommentViewModel;
import com.vuquochung.foodapp.ui.foodlist.FoodListViewModel;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;

public class ViewOrdersFragment extends Fragment implements ILoadOrderCallbackListener {

    @BindView(R.id.recycler_orders)
    RecyclerView recycler_orders;

    AlertDialog dialog;
    private Unbinder unbinder;
    private ViewOrdersViewModel viewOrdersViewModel;
    private ILoadOrderCallbackListener listener;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewOrdersViewModel =
                new ViewModelProvider(this).get(ViewOrdersViewModel.class);

        //binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        //View root = binding.getRoot();
        View root=inflater.inflate(R.layout.fragment_view_order,container,false);
        unbinder=ButterKnife.bind(this,root);
        initViews(root);
        loadOrderFromFirebase();
        viewOrdersViewModel.getMutableLiveDataOrderList().observe(getViewLifecycleOwner(),orderList -> {
            //Log.d("DATAAAAAAAA",orderList.size()+"");
            MyOrdersAdapter adapter=new MyOrdersAdapter(getContext(),orderList);
            recycler_orders.setAdapter(adapter);
        });
        return root;
    }

    private void loadOrderFromFirebase() {
        Log.d("AAAAAAAA",Common.currentUser.getUid());
        List<Order> orderList=new ArrayList<>();
        FirebaseDatabase.getInstance().getReference(Common.ORDER_REF).orderByChild("useId").equalTo(Common.currentUser.getUid()).limitToLast(100).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot orderSnapShot:snapshot.getChildren())
                {
                    Order order=orderSnapShot.getValue(Order.class);
                    order.setOrderNumber(orderSnapShot.getKey());
                    orderList.add(order);
                }
                listener.onLoadOrderSuccess(orderList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onLoadOrderFailed(error.getMessage());
            }
        });
    }

    private void initViews(View root) {
        listener=this;
        dialog=new SpotsDialog.Builder().setCancelable(false).setContext(getContext()).build();
        recycler_orders.setHasFixedSize(true);
        LinearLayoutManager layoutManager=new LinearLayoutManager(getContext());
        recycler_orders.setLayoutManager(layoutManager);
        recycler_orders.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));
    }

    @Override
    public void onLoadOrderSuccess(List<Order> orderList) {
        dialog.dismiss();
        viewOrdersViewModel.setMutableLiveDataOrderList(orderList);
    }

    @Override
    public void onLoadOrderFailed(String message) {
        dialog.dismiss();
        Toast.makeText(getContext(),""+message,Toast.LENGTH_SHORT).show();
    }
}